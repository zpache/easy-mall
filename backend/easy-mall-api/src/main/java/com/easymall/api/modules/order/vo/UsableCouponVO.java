package com.easymall.api.modules.order.vo;

import java.math.BigDecimal;

/**
 * 结算页可用优惠券视图：服务端按当前购物车金额实时算出抵扣与可用性。
 *
 * @param couponUserId   领券记录 id（下单时回传）
 * @param name           券名称
 * @param valueText      面额文案，如 ¥15 / 9折
 * @param ruleText       门槛文案，如 满99元可用 / 无门槛
 * @param discountAmount 本单可抵扣金额
 * @param usable         是否可用（门槛不满足 / 已过期为 false）
 * @param reason         不可用原因
 * @param expireAt       到期时间，null 表示长期有效
 */
public record UsableCouponVO(
        Long couponUserId,
        String name,
        String valueText,
        String ruleText,
        BigDecimal discountAmount,
        boolean usable,
        String reason,
        String expireAt
) {
}
