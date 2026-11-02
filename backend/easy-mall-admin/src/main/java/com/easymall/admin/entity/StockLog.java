package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存流水：每次入库 / 出库 / 盘点都会留一条记录，便于对账与追溯。
 */
@Data
@TableName("mall_stock_log")
public class StockLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long goodsId;

    /** SKU ID：多规格商品的流水记录到 SKU 维度，商品级流水为空 */
    private Long skuId;

    /** 规格值文案，如 红色,S */
    private String specText;

    /** in 入库 / out 出库 / check 盘点 */
    private String type;

    /** 变动数量，正数增加、负数减少 */
    private Integer changeQty;

    private Integer beforeStock;

    private Integer afterStock;

    private String remark;

    private LocalDateTime createdAt;

    /** 操作人 ID */
    private Long createdBy;
}
