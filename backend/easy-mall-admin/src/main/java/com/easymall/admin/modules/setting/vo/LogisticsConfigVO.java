package com.easymall.admin.modules.setting.vo;

import lombok.Data;

/**
 * 物流配置（快递100）视图：密钥只回显脱敏值 + 是否已配置。
 */
@Data
public class LogisticsConfigVO {

    private Boolean kuaidi100Enabled;

    private String kuaidi100Customer;

    /** 脱敏值 */
    private String kuaidi100Key;

    private Boolean kuaidi100KeySet;
}
