package com.easymall.admin.modules.member.vo;

/**
 * 会员积分规则。
 */
public record PointsRuleVO(
        /** 订单支付后是否发放积分 */
        Boolean enabled,
        /** 每消费 1 元发放的积分数 */
        Integer rate
) {
}
