package com.easymall.admin.modules.decorate.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新建装修页面表单。
 */
public record PageForm(
        @NotBlank(message = "页面名称不能为空")
        @Size(max = 100, message = "页面名称不能超过 100 个字符")
        String name,

        @NotNull(message = "页面类型不能为空")
        @Min(value = 1, message = "页面类型只能为 1 首页 / 2 分类页 / 3 微页面")
        @Max(value = 3, message = "页面类型只能为 1 首页 / 2 分类页 / 3 微页面")
        Integer type,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark
) {
}
