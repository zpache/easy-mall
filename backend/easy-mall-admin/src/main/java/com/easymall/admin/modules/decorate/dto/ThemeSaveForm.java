package com.easymall.admin.modules.decorate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 主题保存表单。
 */
public record ThemeSaveForm(
        @NotBlank(message = "主题名称不能为空")
        @Size(max = 50, message = "主题名称不能超过 50 字")
        String name,

        @NotBlank(message = "主题配置不能为空")
        String configJson,

        String remark,

        /** 乐观锁版本号（编辑时携带） */
        Integer version) {
}
