package com.easymall.admin.modules.goods.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品规格与 SKU 保存表单。
 *
 * <p>specs 为空表示把商品改回单规格（会清空全部 SKU，价格与库存沿用商品表）；
 * 非空时 skus 必须覆盖规格的全部组合，否则前端少生成组合会导致商详选不出规格。</p>
 */
public record SkuConfigForm(
        /** 规格定义 JSON：[{"name":"颜色","values":["红","蓝"]}] */
        String specs,
        List<SkuItem> skus
) {

    /**
     * 单个 SKU：带 id 表示更新已有行，不带表示新增。
     */
    public record SkuItem(
            Long id,
            String specText,
            BigDecimal price,
            BigDecimal originalPrice,
            Integer stock,
            String image,
            Integer status
    ) {
    }
}
