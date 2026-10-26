package com.easymall.admin.modules.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理后台账号新增/编辑表单。
 * 编辑时 password 留空表示不修改密码。
 */
public record SysUserForm(

        @NotBlank(message = "账号不能为空")
        @Size(min = 4, max = 32, message = "账号长度需在 4-32 之间")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "账号只能包含字母、数字和下划线")
        String username,

        @Size(min = 6, max = 32, message = "密码长度需在 6-32 之间")
        String password,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 32, message = "昵称长度不能超过 32")
        String nickname,

        @Size(max = 255, message = "头像地址过长")
        String avatar,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "角色不能为空")
        String role,

        @NotNull(message = "状态不能为空")
        Integer status,

        /** 乐观锁版本号：编辑时原样带上详情返回的 version，新增留空 */
        @Min(value = 0, message = "版本号不合法")
        Integer version
) {
}
