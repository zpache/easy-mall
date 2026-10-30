import { requestClient } from '#/api/request';

export namespace SettingsPaymentApi {
  export interface ConfigCheckItem {
    key: string;
    label: string;
    message: string;
    required: boolean;
    /** PASS 通过 / WARN 提醒 / FAIL 阻断 / SKIP 未启用可跳过 */
    status: 'FAIL' | 'PASS' | 'SKIP' | 'WARN';
  }

  export interface PaymentConfig {
    appId: string;
    /** 脱敏值，仅用于展示是否配置 */
    apiV3Key: string;
    apiV3KeySet: boolean;
    /** 发货后自动确认收货天数，0 表示不自动完成 */
    autoReceiveDays: number;
    mchId: string;
    mchSerialNo: string;
    mockPayEnabled: boolean;
    notifyUrl: string;
    orderPayTimeoutMinutes: number;
    /** 脱敏值 */
    privateKey: string;
    privateKeySet: boolean;
    /** 退款结果回调地址，可留空 */
    refundNotifyUrl: string;
    /** 微信支付公钥（脱敏） */
    wxpayPublicKey: string;
    wxpayPublicKeySet: boolean;
    /** 微信支付公钥 ID */
    wxpayPublicKeyId: string;
    wxpayEnabled: boolean;
  }

  export interface PaymentConfigPayload {
    appId: string;
    /** 留空表示不修改 */
    apiV3Key?: string;
    autoReceiveDays?: number;
    mchId: string;
    mchSerialNo: string;
    mockPayEnabled: boolean;
    notifyUrl: string;
    orderPayTimeoutMinutes: number;
    /** 留空表示不修改 */
    privateKey?: string;
    refundNotifyUrl?: string;
    /** 留空表示不修改 */
    wxpayPublicKey?: string;
    wxpayPublicKeyId?: string;
    wxpayEnabled: boolean;
  }
}

/**
 * 支付配置详情
 */
async function getPaymentConfig() {
  return requestClient.get<SettingsPaymentApi.PaymentConfig>('/payment/config');
}

/**
 * 保存支付配置
 */
async function updatePaymentConfig(data: SettingsPaymentApi.PaymentConfigPayload) {
  return requestClient.put('/payment/config', data);
}

/**
 * 支付参数自检
 */
async function checkPaymentConfig() {
  return requestClient.post<SettingsPaymentApi.ConfigCheckItem[]>('/payment/check');
}

export { checkPaymentConfig, getPaymentConfig, updatePaymentConfig };
