package com.easymall.admin.common.context;

/**
 * 当前登录主体（由 JWT 解析得到，不落库查询）。
 *
 * @param userId   管理员 ID
 * @param username 登录账号
 * @param nickname 昵称
 * @param role     角色标识，如 ADMIN
 */
public record LoginUser(Long userId, String username, String nickname, String role) {
}
