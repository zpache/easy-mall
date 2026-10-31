import { requestClient } from '#/api/request';

export namespace SettingsNotifyApi {
  export interface NotifyConfig {
    /** 脱敏值，仅用于展示是否配置 */
    tmplDeliver: string;
    tmplRefund: string;
    subscribeEnabled: boolean;
    wxAppId: string;
    /** 脱敏值 */
    wxAppSecret: string;
    wxAppSecretSet: boolean;
  }

  export interface NotifyConfigPayload {
    tmplDeliver: string;
    tmplRefund: string;
    subscribeEnabled: boolean;
    wxAppId: string;
    /** 留空表示不修改 */
    wxAppSecret?: string;
  }
}

/**
 * 通知配置详情
 */
async function getNotifyConfig() {
  return requestClient.get<SettingsNotifyApi.NotifyConfig>('/notify/config');
}

/**
 * 保存通知配置
 */
async function updateNotifyConfig(data: SettingsNotifyApi.NotifyConfigPayload) {
  return requestClient.put('/notify/config', data);
}

export { getNotifyConfig, updateNotifyConfig };
