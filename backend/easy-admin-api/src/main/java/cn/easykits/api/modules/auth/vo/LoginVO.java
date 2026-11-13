package cn.easykits.api.modules.auth.vo;

import cn.easykits.api.common.context.LoginUser;

/**
 * 登录响应。
 */
public record LoginVO(String accessToken, String tokenType, long expiresIn, LoginUser user) {
}
