package cn.easykits.admin.modules.goods.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品规格与 SKU 配置视图。
 */
public record SkuConfigVO(
        Long goodsId,
        /** 规格定义 JSON，为空表示单规格商品 */
        String specs,
        List<SkuItem> skus
) {

    public record SkuItem(
            Long id,
            String skuCode,
            String specText,
            BigDecimal price,
            BigDecimal originalPrice,
            Integer stock,
            Integer sales,
            String image,
            Integer status
    ) {
    }
}
