package com.easymall.admin.modules.auth.vo;

import com.easymall.admin.common.context.LoginUser;

/**
 * 登录响应。
 *
 * @param accessToken  访问令牌
 * @param refreshToken 刷新令牌。当前为无状态 JWT，与 accessToken 同值；
 *                     前端（vben）要求双 token 结构，刷新端点接入 Redis 黑名单后再独立颁发
 * @param tokenType    令牌类型，固定 Bearer
 * @param expiresIn    有效期（秒）
 * @param user         登录用户信息
 */
public record LoginVO(String accessToken, String refreshToken, String tokenType, long expiresIn, LoginUser user) {
}
