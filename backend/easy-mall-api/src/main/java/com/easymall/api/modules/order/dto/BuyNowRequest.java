package com.easymall.api.modules.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 立即购买下单请求：不走购物车，直接按指定商品与数量生成订单，不影响购物车数据。
 */
public record BuyNowRequest(
        @NotNull(message = "请选择商品") Long goodsId,
        @NotNull(message = "请选择购买数量")
        @Min(value = 1, message = "购买数量至少为1")
        @Max(value = 999, message = "购买数量超出限制") Integer quantity,
        @NotNull(message = "请选择收货地址") Long addressId,
        @Size(max = 255, message = "备注过长") String remark,
        /** 使用的会员券记录 id（mk_coupon_user.id），不传或为 null 表示不使用优惠券 */
        Long couponUserId
) {
}
