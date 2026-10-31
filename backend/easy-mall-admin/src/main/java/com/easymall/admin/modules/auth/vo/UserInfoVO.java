package com.easymall.admin.modules.auth.vo;

import java.util.List;

/**
 * vben 前端 /user/info 接口所需的用户信息结构。
 *
 * @param userId    管理员 ID
 * @param username  登录账号
 * @param realName  显示名（昵称）
 * @param avatar    头像地址，可为空
 * @param roles     角色编码列表（库中单值字符串，包装为数组，前端用于权限判断）
 * @param roleNames 角色名称列表，个人中心展示用，与 roles 按序对应
 * @param homePath  登录后的默认首页，取用户第一个可访问的菜单路径，为空时前端回退默认值
 */
public record UserInfoVO(Long userId, String username, String realName, String avatar,
                         List<String> roles, List<String> roleNames, String homePath) {
}
