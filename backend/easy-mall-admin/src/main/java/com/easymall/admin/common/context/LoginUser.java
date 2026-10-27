package com.easymall.admin.common.context;

import java.util.List;

/**
 * 当前登录主体（由 JWT 解析得到，不落库查询）。
 *
 * @param userId   管理员 ID
 * @param username 登录账号
 * @param nickname 昵称
 * @param roles    角色标识列表，如 ["ADMIN"]；为空表示未分配角色
 */
public record LoginUser(Long userId, String username, String nickname, List<String> roles) {

    public LoginUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    /**
     * 主角色（取第一个），兼容仅需单角色的场景。
     */
    public String primaryRole() {
        return roles.isEmpty() ? null : roles.getFirst();
    }
}
