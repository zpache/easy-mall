package com.easymall.admin.modules.system.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台用户视图。
 */
public record UserVO(
        Long id,
        String username,
        String nickname,
        String phone,
        String email,
        Long deptId,
        String deptName,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        List<Long> roleIds,
        List<String> roleNames,
        Integer version
) {
}
