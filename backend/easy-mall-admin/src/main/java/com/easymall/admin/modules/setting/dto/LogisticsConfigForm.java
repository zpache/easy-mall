package com.easymall.admin.modules.setting.dto;

/**
 * 物流配置（快递100）表单。
 * <p>
 * key 为敏感项：留空表示不修改，传 {@link com.easymall.admin.modules.setting.service.LogisticsConfigService#CLEAR} 表示清空。
 */
public record LogisticsConfigForm(
        Boolean kuaidi100Enabled,

        String kuaidi100Customer,

        String kuaidi100Key
) {
}
