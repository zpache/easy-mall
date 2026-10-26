package com.easymall.admin.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 配置项。
 */
@Data
@ConfigurationProperties(prefix = "easy-mall.jwt")
public class JwtProperties {

    /** 签名密钥，长度至少 32 字节。生产环境务必通过环境变量注入 */
    private String secret;

    /** 签发者 */
    private String issuer = "easy-mall-admin";

    /** 管理端令牌有效期 */
    private Duration adminExpire = Duration.ofHours(12);
}
