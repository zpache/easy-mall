package com.easymall.api.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 免登录接口白名单（相对 context-path /api 的路径）。
 */
@Data
@ConfigurationProperties(prefix = "easy-mall.security")
public class SecurityProperties {

    private List<String> ignoreUrls = new ArrayList<>();
}
