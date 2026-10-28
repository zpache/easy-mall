package com.easymall.admin.modules.decorate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 保存装修草稿表单：整页 JSON 覆盖写入。
 */
public record PageSaveForm(
        @Size(max = 100, message = "页面名称不能超过 100 个字符")
        String name,

        @NotBlank(message = "页面结构不能为空")
        String schemaJson,

        /** 乐观锁版本号 */
        Integer version
) {
}
