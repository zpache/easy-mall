package com.easymall.api.modules.cart.vo;

import java.math.BigDecimal;

/**
 * 购物车条目视图（已聚合商品信息）。
 *
 * @param invalid 商品已下架或库存不足时为 true，前端置灰处理
 */
public record CartItemVO(
        Long id,
        Long goodsId,
        String name,
        String mainImage,
        BigDecimal price,
        Integer stock,
        Integer quantity,
        Integer checked,
        BigDecimal subtotal,
        boolean invalid
) {
}
