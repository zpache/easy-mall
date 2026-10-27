package com.easymall.admin.modules.member.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 会员积分人工调整表单：变动量正数为发放、负数为扣减。
 */
public record PointsAdjustForm(
        @NotNull(message = "积分变动量不能为空")
        Integer change,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark
) {
}
