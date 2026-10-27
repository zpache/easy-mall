package com.easymall.admin.modules.system.vo;

import java.util.List;

/**
 * 供前端 vben 动态路由使用的菜单节点，字段与 vue-router 的路由记录对齐。
 * <p>
 * 与 {@link MenuVO} 的区别：这里只输出路由需要的字段，不含 id/pid/status 等管理端字段，
 * 前端拿到后可直接交给 generateAccessible 生成路由与菜单。
 */
public record MenuRouteVO(
        String name,
        String path,
        String component,
        String redirect,
        MenuMeta meta,
        List<MenuRouteVO> children
) {
}
