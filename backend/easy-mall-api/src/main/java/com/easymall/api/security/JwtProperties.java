package com.easymall.api.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 配置项。
 */
@Data
@ConfigurationProperties(prefix = "easy-mall.jwt")
public class JwtProperties {

    /** 签名密钥，长度至少 32 字节 */
    private String secret;

    private String issuer = "easy-mall-api";

    /** 小程序端令牌有效期，默认 7 天 */
    private Duration expire = Duration.ofDays(7);
}
