package com.easymall.admin.modules.decorate.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 保存导航装修配置：三段 JSON 整体覆盖。
 */
public record NavSaveForm(
        @NotBlank(message = "底部 TabBar 配置不能为空")
        String tabbarJson,

        @NotBlank(message = "顶部导航配置不能为空")
        String navbarJson,

        @NotBlank(message = "快捷入口配置不能为空")
        String floatJson,

        /** 乐观锁版本号 */
        Integer version
) {
}
