package com.easymall.admin.modules.decorate.vo;

/**
 * 真机扫码预览返回：页面名 + 渲染结构。
 */
public record PagePreviewVO(
        Long id,
        String name,
        String schemaJson,
        String previewCode,
        Integer status
) {
}
