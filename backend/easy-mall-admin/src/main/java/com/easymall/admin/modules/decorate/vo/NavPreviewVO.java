package com.easymall.admin.modules.decorate.vo;

/**
 * 免登录预览用的导航配置：只暴露渲染所需的三段 JSON。
 */
public record NavPreviewVO(
        String tabbarJson,
        String navbarJson,
        String floatJson
) {
}
