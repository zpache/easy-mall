package com.easymall.admin.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 角色新增 / 编辑表单。
 *
 * @param name        角色名称
 * @param code        角色标识（ADMIN / OPERATOR 等），会作为 Spring Security 的 authority
 * @param sort        排序值
 * @param status      1 正常，0 禁用
 * @param remark      备注
 * @param permissions 授权的菜单 ID 列表
 */
public record RoleForm(
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 50, message = "角色名称不能超过 50 个字符")
        String name,

        @NotBlank(message = "角色标识不能为空")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$", message = "角色标识需为大写字母、数字或下划线，且以字母开头")
        String code,

        Integer sort,

        Integer status,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark,

        List<Long> permissions
) {
}
