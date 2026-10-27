package com.easymall.admin.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 菜单新增 / 编辑表单。meta 结构与 vben 路由元信息对齐，其余字段为路由本体。
 */
public record MenuForm(
        Long pid,

        @NotBlank(message = "路由名称不能为空")
        String name,

        @NotBlank(message = "菜单类型不能为空")
        @Pattern(regexp = "catalog|menu|button|link|embedded", message = "菜单类型不合法")
        String type,

        String authCode,

        String path,

        String component,

        Integer status,

        MenuMetaForm meta
) {

    /**
     * 菜单 meta 表单。
     */
    public record MenuMetaForm(
            String title,
            String icon,
            Integer order,
            String badge,
            String badgeType,
            String badgeVariants,
            Boolean keepAlive,
            Boolean affixTab,
            Boolean hideInMenu,
            Boolean hideChildrenInMenu,
            Boolean hideInBreadcrumb,
            Boolean hideInTab,
            String link,
            String iframeSrc
    ) {
    }
}
