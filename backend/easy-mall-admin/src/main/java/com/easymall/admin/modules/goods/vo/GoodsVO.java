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
        Integer sales,
        String unit,
        String detail,
        Integer status,
        Integer sort,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version
) {
}
