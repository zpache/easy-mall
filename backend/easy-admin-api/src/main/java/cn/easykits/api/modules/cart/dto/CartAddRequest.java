package cn.easykits.api.modules.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 加入购物车请求。
 * <p>多规格商品必须带 skuId，单规格商品不传。</p>
 */
public record CartAddRequest(
        @NotNull(message = "商品 ID 不能为空") Long goodsId,
        @NotNull(message = "数量不能为空") @Min(value = 1, message = "数量至少为 1") Integer quantity,
        /** SKU ID：多规格商品必填 */
        Long skuId
) {
}
