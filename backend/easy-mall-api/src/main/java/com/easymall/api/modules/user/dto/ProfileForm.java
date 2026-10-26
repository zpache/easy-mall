package com.easymall.api.modules.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 用户资料修改表单。
 */
public record ProfileForm(
        @Size(max = 64, message = "昵称过长") String nickname,
        @Size(max = 255, message = "头像地址过长") String avatar,
        @Min(value = 0, message = "性别值不正确") @Max(value = 2, message = "性别值不正确") Integer gender
) {
}
