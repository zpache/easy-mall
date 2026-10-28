package com.easymall.api.modules.coupon.vo;

/**
 * 领券中心的券卡片（装修「优惠券」组件消费）。
 * 面额与规则文案由后端统一生成，避免两端各写一套格式化逻辑。
 *
 * <p>对外标识是随机码 {@code code}，不下发自增主键。</p>
 *
 * @param code       券的对外随机码，领取时作为路径参数
 * @param valueText  面额，如 “¥15”“9折”
 * @param ruleText   门槛，如 “满99元可用”“无门槛”
 * @param validText  有效期，如 “2026.10.01-2026.11.11”“领取后7天有效”
 * @param remainCount 剩余可领张数，-1 表示不限量
 * @param perLimit   每人限领张数
 * @param soldOut    是否已领完
 */
public record CouponCardVO(
        String code,
        String name,
        Integer type,
        String valueText,
        String ruleText,
        String validText,
        Integer totalCount,
        Integer remainCount,
        Integer perLimit,
        Boolean soldOut
) {
}
