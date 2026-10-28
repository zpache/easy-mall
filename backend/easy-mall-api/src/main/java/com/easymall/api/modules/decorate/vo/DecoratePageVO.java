package com.easymall.api.modules.decorate.vo;

/**
 * 小程序端渲染用的装修页面：页面结构 + 全局导航配置。
 *
 * @param schemaJson 已发布快照（未发布时回退为当前草稿）
 * @param nav        底部 TabBar / 顶部导航 / 悬浮球
 */
public record DecoratePageVO(
        Long id,
        String name,
        Integer type,
        String schemaJson,
        NavConfigVO nav
) {
}
