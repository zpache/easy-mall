package com.easymall.admin.modules.setting.vo;

import lombok.Data;

/**
 * 通知配置（微信订阅消息）视图：secret 只回显脱敏值 + 是否已配置。
 */
@Data
public class NotifyConfigVO {

    private Boolean subscribeEnabled;

    private String wxAppId;

    /** 脱敏值 */
    private String wxAppSecret;

    private Boolean wxAppSecretSet;

    private String tmplDeliver;

    private String tmplRefund;
}
