package com.easymall.admin.modules.system.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色视图。
 *
 * @param permissions 已授权的菜单 ID 列表
 */
public record RoleVO(
        Long id,
        String name,
        String code,
        Integer sort,
        Integer status,
        String remark,
        LocalDateTime createdAt,
        List<Long> permissions,
        Integer version
) {
}
