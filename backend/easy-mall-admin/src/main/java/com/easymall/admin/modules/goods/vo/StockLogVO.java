package com.easymall.admin.modules.goods.vo;

import java.time.LocalDateTime;

/**
 * 库存流水视图，附带商品编码与名称便于列表展示。
 */
public record StockLogVO(
        Long id,
        Long goodsId,
        String goodsCode,
        String goodsName,
        String type,
        Integer changeQty,
        Integer beforeStock,
        Integer afterStock,
        String remark,
        LocalDateTime createdAt,
        Long createdBy
) {
}
