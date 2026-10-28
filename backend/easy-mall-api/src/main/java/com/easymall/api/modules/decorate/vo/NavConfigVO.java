package com.easymall.api.modules.decorate.vo;

/**
 * 导航装修三段渲染配置，前端按同一口径解析。
 */
public record NavConfigVO(
        String tabbarJson,
        String navbarJson,
        String floatJson
) {
}
