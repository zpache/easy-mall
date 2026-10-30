package com.easymall.admin.modules.goods.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品视图：在实体之外补齐分类名、品牌名，列表页无需二次查询。
 */
public record GoodsVO(
        Long id,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        String name,
        String subtitle,
        String mainImage,
        String images,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer stock,
        Integer stockWarn,
        /** 真实销量 */
        Integer sales,
        /** 基础销量（运营维护的展示基数） */
        Integer baseSales,
        String unit,
        String detail,
        Integer status,
        Integer sort,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version
) {
}
