package com.easymall.api.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 微信 jscode2session 响应。
 */
public record WxSessionResponse(
        String openid,
        @JsonProperty("session_key") String sessionKey,
        String unionid,
        Integer errcode,
        String errmsg
) {
    public boolean success() {
        return openid != null && !openid.isBlank();
    }
}
