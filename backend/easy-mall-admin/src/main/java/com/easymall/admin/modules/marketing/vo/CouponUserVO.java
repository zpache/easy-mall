package com.easymall.admin.modules.marketing.vo;

import java.time.LocalDateTime;

/**
 * 领券记录视图：补齐券名称与会员昵称。
 */
public record CouponUserVO(
        Long id,
        Long couponId,
        String couponName,
        Long userId,
        String nickname,
        Integer status,
        String orderNo,
        LocalDateTime receivedAt,
        LocalDateTime usedAt
) {
}
