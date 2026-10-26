package com.easymall.api.common.context;

/**
 * 当前登录的小程序用户。
 *
 * @param userId   用户 ID
 * @param openid   微信 openid
 * @param nickname 昵称
 */
public record LoginUser(Long userId, String openid, String nickname) {
}
