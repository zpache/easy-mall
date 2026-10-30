import { requestClient } from '#/api/request';

export namespace SettingsLogisticsApi {
  export interface LogisticsConfig {
    kuaidi100Customer: string;
    /** 脱敏值，仅用于展示是否配置 */
    kuaidi100Key: string;
    kuaidi100KeySet: boolean;
    kuaidi100Enabled: boolean;
  }

  export interface LogisticsConfigPayload {
    kuaidi100Customer: string;
    /** 留空表示不修改 */
    kuaidi100Key?: string;
    kuaidi100Enabled: boolean;
  }
}

/**
 * 物流配置详情
 */
async function getLogisticsConfig() {
  return requestClient.get<SettingsLogisticsApi.LogisticsConfig>(
    '/logistics/config',
  );
}

/**
 * 保存物流配置
 */
async function updateLogisticsConfig(
  data: SettingsLogisticsApi.LogisticsConfigPayload,
) {
  return requestClient.put('/logistics/config', data);
}

export { getLogisticsConfig, updateLogisticsConfig };
