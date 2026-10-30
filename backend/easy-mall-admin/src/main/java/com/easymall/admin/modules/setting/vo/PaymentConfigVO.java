package com.easymall.admin.modules.setting.vo;

import lombok.Data;

/**
 * 支付配置详情。敏感项（APIv3 密钥 / 商户私钥）只返回脱敏值，
 * 前端通过 *Set 标记判断是否已配置。
 */
@Data
public class PaymentConfigVO {

    /** 是否启用微信支付渠道 */
    private Boolean wxpayEnabled;

    /** 是否启用模拟支付 */
    private Boolean mockPayEnabled;

    /** 小程序 AppID */
    private String appId;

    /** 微信支付商户号 */
    private String mchId;

    /** 商户 API 证书序列号 */
    private String mchSerialNo;

    /** APIv3 密钥（脱敏，形如 ********） */
    private String apiV3Key;

    /** 是否已配置 APIv3 密钥 */
    private Boolean apiV3KeySet;

    /** 商户 API 私钥（脱敏） */
    private String privateKey;

    /** 是否已配置商户 API 私钥 */
    private Boolean privateKeySet;

    /** 支付结果回调地址 */
    private String notifyUrl;

    /** 微信支付公钥（脱敏） */
    private String wxpayPublicKey;

    /** 是否已配置微信支付公钥 */
    private Boolean wxpayPublicKeySet;

    /** 微信支付公钥 ID */
    private String wxpayPublicKeyId;

    /** 退款结果回调地址 */
    private String refundNotifyUrl;

    /** 订单支付超时时长（分钟） */
    private Integer orderPayTimeoutMinutes;

    /** 发货后自动确认收货天数，0 表示不自动完成 */
    private Integer autoReceiveDays;
}
