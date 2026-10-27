package com.easymall.admin.modules.system.vo;

import java.util.List;

/**
 * 菜单树节点。
 */
public record MenuVO(
        Long id,
        Long pid,
        String name,
        String type,
        String authCode,
        String path,
        String component,
        Integer status,
        MenuMeta meta,
        List<MenuVO> children
) {
}
