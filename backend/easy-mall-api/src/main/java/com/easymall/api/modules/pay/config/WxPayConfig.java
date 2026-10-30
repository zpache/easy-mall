package com.easymall.api.modules.pay.config;

/**
 * 微信支付 v3 运行期配置（来自「商城设置 - 支付配置」，不在 yml 里写死）。
 *
 * @param enabled       是否启用微信支付渠道
 * @param mockEnabled   是否启用模拟支付（开发联调 / 未接真实商户号时使用）
 * @param appId         小程序 AppID
 * @param mchId         商户号
 * @param mchSerialNo   商户 API 证书序列号
 * @param apiV3Key      APIv3 密钥（32 位，用于解密回调报文）
 * @param privateKey    商户 API 私钥 PEM（请求签名）
 * @param notifyUrl     支付结果回调地址（https）
 * @param publicKey     微信支付公钥 PEM（回调验签）
 * @param publicKeyId   微信支付公钥 ID（PUB_KEY_ID_ 开头）
 */
public record WxPayConfig(
        boolean enabled,
        boolean mockEnabled,
        String appId,
        String mchId,
        String mchSerialNo,
        String apiV3Key,
        String privateKey,
        String notifyUrl,
        String publicKey,
        String publicKeyId
) {

    /** 商户下单参数是否齐全（缺任意一项都无法发起真实支付） */
    public boolean complete() {
        return filled(appId) && filled(mchId) && filled(mchSerialNo)
                && filled(apiV3Key) && filled(privateKey) && filled(notifyUrl);
    }

    /** 回调验签所需参数是否齐全 */
    public boolean notifyReady() {
        return filled(apiV3Key) && filled(publicKey);
    }

    private static boolean filled(String value) {
        return value != null && !value.isBlank();
    }
}
