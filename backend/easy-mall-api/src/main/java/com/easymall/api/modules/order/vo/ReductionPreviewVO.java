package com.easymall.api.modules.order.vo;

import java.math.BigDecimal;

/**
 * 结算页满减预估：展示当前商品金额下每个生效活动的命中情况。
 */
public record ReductionPreviewVO(
        Long reductionId,
        String name,
        /** 1 阶梯满减 2 每满减 */
        Integer mode,
        BigDecimal threshold,
        BigDecimal benefit,
        /** 命中次数：每满减按金额可叠加多次，阶梯为 1 */
        Integer times,
        /** 预计优惠金额 */
        BigDecimal discount,
        /** 是否满足门槛 */
        boolean usable
) {
}
