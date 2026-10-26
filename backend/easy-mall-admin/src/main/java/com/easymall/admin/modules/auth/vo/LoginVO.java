package com.easymall.admin.modules.auth.vo;

import com.easymall.admin.common.context.LoginUser;

/**
 * 登录响应。
 *
 * @param accessToken 访问令牌
 * @param tokenType   令牌类型，固定 Bearer
 * @param expiresIn   有效期（秒）
 * @param user        登录用户信息
 */
public record LoginVO(String accessToken, String tokenType, long expiresIn, LoginUser user) {
}
