package com.easymall.api.security;

import com.easymall.api.common.context.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * 小程序自定义登录态：token 只是随机串，会话数据存 Redis。
 *
 * <p>这是微信官方推荐的会话管理模式：wx.login 的 code 换到 openid 后，
 * 服务端生成不可预测的自定义登录态下发给客户端，有效期、续期、踢下线都由服务端控制。
 * 相比自包含的 JWT：可随时作废（logout / 单设备挤下线）、可滑动续期、不暴露任何用户信息。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSessionService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SessionProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Redis 中的会话体。nickname 仅作登录响应兜底，权威资料以 m_member 为准。
     */
    record SessionData(Long userId, String openid, String nickname) {
    }

    /**
     * 创建登录态，返回下发给客户端的不透明 token。
     */
    public String create(LoginUser loginUser) {
        String token = newToken();
        Duration ttl = properties.getTtl();
        String json;
        try {
            json = objectMapper.writeValueAsString(
                    new SessionData(loginUser.userId(), loginUser.openid(), loginUser.nickname()));
        } catch (JacksonException e) {
            log.error("序列化登录会话失败", e);
            throw new IllegalStateException("登录态序列化失败", e);
        }
        redisTemplate.opsForValue().set(sessionKey(token), json, ttl);
        if (properties.isSingleDevice()) {
            // 挤下线：同一用户保留最新一个登录态
            String userKey = userKey(loginUser.userId());
            String previous = redisTemplate.opsForValue().get(userKey);
            if (StringUtils.hasText(previous) && !previous.equals(token)) {
                redisTemplate.delete(sessionKey(previous));
            }
            redisTemplate.opsForValue().set(userKey, token, ttl);
        }
        return token;
    }

    /**
     * 校验 token 并滑动续期；不存在 / 已过期 / 已被踢下线均返回 empty。
     */
    public Optional<LoginUser> resolve(String token) {
        String key = sessionKey(token);
        String json = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }
        try {
            SessionData data = objectMapper.readValue(json, SessionData.class);
            redisTemplate.expire(key, properties.getTtl());
            if (properties.isSingleDevice()) {
                redisTemplate.expire(userKey(data.userId()), properties.getTtl());
            }
            return Optional.of(new LoginUser(data.userId(), data.openid(), data.nickname()));
        } catch (JacksonException e) {
            // 数据损坏等同未登录，顺手清掉脏数据
            log.warn("登录会话数据损坏，已清除: key={}", key);
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    /**
     * 退出登录：删除会话与本机映射。
     */
    public void remove(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        String json = redisTemplate.opsForValue().get(sessionKey(token));
        redisTemplate.delete(sessionKey(token));
        if (json != null) {
            try {
                SessionData data = objectMapper.readValue(json, SessionData.class);
                String userKey = userKey(data.userId());
                if (token.equals(redisTemplate.opsForValue().get(userKey))) {
                    redisTemplate.delete(userKey);
                }
            } catch (JacksonException e) {
                log.warn("退出登录时会话数据已损坏，仅删除 token", e);
            }
        }
    }

    public long getExpireSeconds() {
        return properties.getTtl().toSeconds();
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sessionKey(String token) {
        return properties.getKeyPrefix() + "token:" + token;
    }

    private String userKey(Long userId) {
        return properties.getKeyPrefix() + "user:" + userId;
    }
}
