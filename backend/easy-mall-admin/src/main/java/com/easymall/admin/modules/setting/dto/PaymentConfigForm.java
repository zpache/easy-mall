package com.easymall.admin.modules.setting.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 支付配置表单。
 * 敏感项（apiV3Key / privateKey）留空表示「不修改」，传 CLEAR 表示清空。
 */
public record PaymentConfigForm(

        Boolean wxpayEnabled,

        Boolean mockPayEnabled,

        @Size(max = 64, message = "AppID 长度不能超过 64")
        String appId,

        @Size(max = 64, message = "商户号长度不能超过 64")
        String mchId,

        @Size(max = 128, message = "证书序列号长度不能超过 128")
        String mchSerialNo,

        /** 留空 = 不修改 */
        String apiV3Key,

        /** 留空 = 不修改 */
        String privateKey,

        @Size(max = 255, message = "回调地址长度不能超过 255")
        String notifyUrl,

        /** 微信支付公钥 PEM，留空 = 不修改 */
        String wxpayPublicKey,

        @Size(max = 64, message = "公钥 ID 长度不能超过 64")
        String wxpayPublicKeyId,

        @Size(max = 255, message = "退款回调地址长度不能超过 255")
        String refundNotifyUrl,

        @Min(value = 1, message = "支付超时时间至少 1 分钟")
        @Max(value = 1440, message = "支付超时时间最多 1440 分钟")
        Integer orderPayTimeoutMinutes,

        @Min(value = 0, message = "自动确认收货天数不能小于 0")
        @Max(value = 90, message = "自动确认收货天数最多 90 天")
        Integer autoReceiveDays
) {
}
