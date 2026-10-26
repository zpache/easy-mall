package com.easymall.api.modules.auth.vo;

import com.easymall.api.common.context.LoginUser;

/**
 * 登录响应。
 */
public record LoginVO(String accessToken, String tokenType, long expiresIn, LoginUser user) {
}
