package com.easymall.admin.modules.goods.vo;

import java.time.LocalDateTime;

/**
 * 库存列表视图：商品基础信息 + 库存与预警状态。
 *
 * @param lowStock 是否低于预警值（库存 &lt;= 预警值）
 */
public record StockVO(
        Long goodsId,
        String name,
        String mainImage,
        String unit,
        String categoryName,
        String brandName,
        Integer stock,
        Integer stockWarn,
        Integer status,
        Boolean lowStock,
        LocalDateTime updatedAt,
        Integer version
) {
}
