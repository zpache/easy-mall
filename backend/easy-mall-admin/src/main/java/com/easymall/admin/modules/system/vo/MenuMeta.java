package com.easymall.admin.modules.system.vo;

/**
 * 菜单元信息，字段与 vben 路由 meta 对齐。
 */
public record MenuMeta(
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
