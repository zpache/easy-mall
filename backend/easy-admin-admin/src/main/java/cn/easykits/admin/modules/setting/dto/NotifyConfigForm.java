package cn.easykits.admin.modules.setting.dto;

/**
 * 通知配置（微信订阅消息）表单。
 * <p>
 * secret 为敏感项：留空表示不修改，传 {@link NotifyConfigService#CLEAR} 表示清空。
 */
public record NotifyConfigForm(
        Boolean subscribeEnabled,

        String wxAppId,

        String wxAppSecret,

        String tmplDeliver,

        String tmplRefund
) {
}
