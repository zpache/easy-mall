package com.easymall.admin.modules.auth.vo;

import java.util.List;

/**
 * vben 前端 /user/info 接口所需的用户信息结构。
 *
 * @param userId   管理员 ID
 * @param username 登录账号
 * @param realName 显示名（昵称）
 * @param avatar   头像地址，可为空
 * @param roles    角色列表（库中单值字符串，包装为数组）
 */
public record UserInfoVO(Long userId, String username, String realName, String avatar, List<String> roles) {
}
