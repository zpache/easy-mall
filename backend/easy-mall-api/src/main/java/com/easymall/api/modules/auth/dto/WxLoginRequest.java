package com.easymall.api.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信登录请求。
 *
 * @param code 小程序端 wx.login 获取的临时登录凭证
 */
public record WxLoginRequest(
        @NotBlank(message = "code 不能为空") String code
) {
}
