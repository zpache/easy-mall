package com.easymall.api.modules.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信小程序配置。
 */
@Data
@ConfigurationProperties(prefix = "easy-mall.wx")
public class WxProperties {

    private String appId;

    private String appSecret;

    private String sessionUrl = "https://api.weixin.qq.com/sns/jscode2session";

    /**
     * 本地开发/联调用：不走微信服务器，直接用 code 伪造 openid。
     * 生产环境必须为 false。
     */
    private boolean mock = false;
}
