package com.easymall.admin.modules.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 后台用户新增 / 编辑表单。
 * 新增时 password 必填；编辑时留空表示不修改密码。
 */
public record UserForm(

        @NotBlank(message = "登录账号不能为空")
        @Size(min = 4, max = 32, message = "登录账号长度需在 4-32 之间")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "登录账号只能包含字母、数字和下划线")
        String username,

        @Size(min = 6, max = 32, message = "密码长度需在 6-32 之间")
        String password,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 32, message = "姓名不能超过 32 个字符")
        String nickname,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Email(message = "邮箱格式不正确")
        String email,

        Long deptId,

        Integer status,

        List<Long> roleIds
) {
}
