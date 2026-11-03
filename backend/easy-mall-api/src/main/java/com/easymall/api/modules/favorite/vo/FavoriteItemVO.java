package com.easymall.api.modules.favorite.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏列表项：收藏时间 + 商品快照（下架/删除商品保留展示但标记不可购买）。
 */
public record FavoriteItemVO(
        Long goodsId,
        String goodsName,
        String mainImage,
        BigDecimal price,
        BigDecimal originalPrice,
        /** 商品当前是否上架（0 下架/已删除，1 上架） */
        Integer goodsStatus,
        LocalDateTime favoritedAt) {
}
