package com.easymall.api.common.lock;

import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;

/**
 * 基于 Redis 的分布式锁（SET NX PX + 校验持有者释放）。
 *
 * <p>用途：把「检查再写入」的竞态操作串行化，例如同一用户并发领券。
 * 锁有 TTL 兜底（持有者宕机自动过期），释放时用 Lua 脚本比对持有者令牌，
 * 只删自己加的锁，避免误删已超时被他人持有的锁。</p>
 *
 * <p>重要：调用方应保证 {@code action} 里的数据库事务在锁内完成并提交——
 * 即用 TransactionTemplate 在 action 里显式开事务，而不是把本方法包在
 * &#64;Transactional 方法里（那样锁会在事务提交前被释放，串行化失效）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private static final String KEY_PREFIX = "easy-mall:api:lock:";

    /** 释放锁：值（持有者令牌）匹配才删除 */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 在分布式锁内执行操作。
     *
     * @param key  业务键（自动加统一前缀），粒度越小并发越好，如 coupon:receive:{userId}:{code}
     * @param ttl  锁自动过期时间，必须覆盖 action 的最长执行时间（持有者宕机兜底）
     * @param wait 获取锁最长等待时间，超时抛 REQUEST_BUSY，不打无限排队
     * @return action 的返回值
     */
    public <T> T execute(String key, Duration ttl, Duration wait, Supplier<T> action) {
        String lockKey = KEY_PREFIX + key;
        String token = newToken();
        if (!tryAcquire(lockKey, token, ttl, wait)) {
            log.warn("分布式锁获取超时: key={}, wait={}ms", lockKey, wait.toMillis());
            throw new BusinessException(ResultCode.REQUEST_BUSY);
        }
        try {
            return action.get();
        } finally {
            release(lockKey, token);
        }
    }

    private boolean tryAcquire(String lockKey, String token, Duration ttl, Duration wait) {
        long deadline = System.nanoTime() + wait.toNanos();
        while (true) {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, token, ttl);
            if (Boolean.TRUE.equals(ok)) {
                return true;
            }
            if (System.nanoTime() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private void release(String lockKey, String token) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), token);
        } catch (Exception e) {
            // 释放失败只影响锁的可用时长（TTL 到期自动过期），不影响已完成的业务结果
            log.warn("分布式锁释放失败: key={}", lockKey, e);
        }
    }

    private String newToken() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
