package com.easymall.api.modules.coupon.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的优惠券视图。
 *
 * @param status    归一后的状态：0 可用 1 已使用 2 已过期（未使用但已过期的记录也会归一为 2）
 * @param valueText 面额展示文案，如 “¥15”“9折”
 * @param ruleText  使用门槛文案，如 “满99元可用”“无门槛”
 */
public record MemberCouponVO(
        Long id,
        Long couponId,
        String name,
        Integer type,
        BigDecimal amount,
        BigDecimal discount,
        BigDecimal threshold,
        Integer status,
        String valueText,
        String ruleText,
        String orderNo,
        LocalDateTime receivedAt,
        LocalDateTime usedAt,
        LocalDateTime expireAt
) {
}
