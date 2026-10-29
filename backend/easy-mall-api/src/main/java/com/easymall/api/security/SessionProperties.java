package com.easymall.api.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 小程序自定义登录态配置（存储在 Redis）。
 */
@Data
@ConfigurationProperties(prefix = "easy-mall.session")
public class SessionProperties {

    /** 登录态有效期，默认 7 天；每次请求滑动续期 */
    private Duration ttl = Duration.ofDays(7);

    /** Redis key 前缀 */
    private String keyPrefix = "easy-mall:api:session:";

    /** 单设备登录：同一用户再次登录时使旧登录态失效（微信官方推荐的会话管理方式） */
    private boolean singleDevice = true;
}
