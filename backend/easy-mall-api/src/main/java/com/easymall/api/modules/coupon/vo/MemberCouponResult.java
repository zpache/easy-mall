package com.easymall.api.modules.coupon.vo;

import java.util.List;

/**
 * 我的优惠券：各状态数量 + 列表。
 *
 * @param available 可用张数（个人中心角标用）
 * @param used      已使用张数
 * @param expired   已过期张数
 */
public record MemberCouponResult(
        int available,
        int used,
        int expired,
        List<MemberCouponVO> list
) {
}
