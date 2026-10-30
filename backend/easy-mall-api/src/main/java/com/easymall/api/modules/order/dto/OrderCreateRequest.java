package com.easymall.api.modules.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 下单请求：把购物车中已勾选的商品生成订单。
 */
public record OrderCreateRequest(
        @NotNull(message = "请选择收货地址") Long addressId,
        @Size(max = 255, message = "备注过长") String remark,
        /** 使用的会员券记录 id（mk_coupon_user.id），不传或为 null 表示不使用优惠券 */
        Long couponUserId
) {
}
