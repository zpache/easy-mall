package com.easymall.api.modules.pay.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 小程序调起支付所需参数。
 *
 * <p>mock=true 表示当前走模拟支付（未启用微信支付且模拟支付开启），
 * 前端拿到后直接当支付成功处理，无需调 wx.requestPayment。</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PrepayVO(
        boolean mock,
        String appId,
        String timeStamp,
        String nonceStr,
        /** 微信要求字段名就是 package，Java 里只能换个字段名 + 注解 */
        @JsonProperty("package") String packageValue,
        String signType,
        String paySign
) {

    public static PrepayVO mockResult() {
        return new PrepayVO(true, null, null, null, null, null, null);
    }
}
