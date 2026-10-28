package com.easymall.admin.modules.decorate.vo;

/**
 * 装修页面列表视图：不携带整页 JSON，避免列表接口过大。
 */
public record PageListVO(
        Long id,
        String name,
        Integer type,
        Integer status,
        Integer isHome,
        String previewCode,
        String remark,
        String publishedAt,
        String updatedAt,
        Integer version
) {
}
