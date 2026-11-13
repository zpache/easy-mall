package cn.easykits.admin.modules.auth.service;

import cn.easykits.admin.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防爆破守卫：同一账号连续失败 N 次后锁定一段时间。
 * <p>admin 模块无 Redis，进程内 ConcurrentHashMap 计数（单实例部署够用；
 * 多实例部署时需迁到 Redis）。成功登录即清零；过期条目惰性清理。</p>
 */
@Service
public class LoginGuardService {

    /** 连续失败阈值 */
    private static final int MAX_FAILURES = 5;
    /** 锁定时长（毫秒）：15 分钟 */
    private static final long LOCK_MILLIS = 15 * 60 * 1000L;
    /** 计数保留时长：超过后从 0 重新计数（毫秒），30 分钟 */
    private static final long FAILURE_WINDOW_MILLIS = 30 * 60 * 1000L;

    private record FailState(int count, long lastFailAt, long lockUntil) {
    }

    private final Map<String, FailState> states = new ConcurrentHashMap<>();

    /**
     * 登录前检查：账号处于锁定期则直接拒绝。
     */
    public void checkLocked(String username) {
        FailState state = states.get(username);
        if (state == null) {
            return;
        }
        long remain = state.lockUntil() - System.currentTimeMillis();
        if (remain > 0) {
            throw new BusinessException("登录失败次数过多，账号已锁定，请约 %d 分钟后再试".formatted(
                    (remain + 59_999) / 60_000));
        }
    }

    /**
     * 登录失败：累计计数，达到阈值锁定。
     *
     * @return 剩余可尝试次数
     */
    public int recordFailure(String username) {
        long now = System.currentTimeMillis();
        FailState state = states.compute(username, (k, old) -> {
            if (old == null || now - old.lastFailAt() > FAILURE_WINDOW_MILLIS) {
                return new FailState(1, now, 0);
            }
            int count = old.count() + 1;
            return new FailState(count, now, count >= MAX_FAILURES ? now + LOCK_MILLIS : 0);
        });
        // 计数条目到窗口外由下次 compute 惰性清理；这里额外兜底清理过期的锁
        if (states.size() > 10_000) {
            states.entrySet().removeIf(e ->
                    now - e.getValue().lastFailAt() > FAILURE_WINDOW_MILLIS
                            && e.getValue().lockUntil() < now);
        }
        return Math.max(MAX_FAILURES - state.count(), 0);
    }

    /**
     * 登录成功：清零。
     */
    public void recordSuccess(String username) {
        states.remove(username);
    }
}
