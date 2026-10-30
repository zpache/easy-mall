package com.easymall.admin.modules.member.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 会员积分规则表单。
 */
public record PointsRuleForm(

        /** 订单支付后是否发放积分 */
        Boolean enabled,

        /** 每消费 1 元发放的积分数，0 表示不发放 */
        @Min(value = 0, message = "积分比例不能小于 0")
        @Max(value = 100, message = "积分比例过大，请确认（最多 100）")
        Integer rate
) {
}
