package com.easymall.admin.modules.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 会员标签新增 / 编辑表单。
 */
public record TagForm(
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 50, message = "标签名称不能超过 50 个字符")
        String name,

        @Size(max = 20, message = "颜色值不能超过 20 个字符")
        String color,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark,

        /** 乐观锁版本号，编辑时携带 */
        Integer version
) {
}
