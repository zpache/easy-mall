package com.easymall.api.modules.decorate.vo;

/**
 * 线上主题：小程序端换肤用。
 *
 * @param name      主题名称
 * @param themeJson 已发布主题配置快照；无启用主题时为 null，小程序端回退内置默认配色
 */
public record ThemeConfigVO(
        String name,
        String themeJson) {
}
