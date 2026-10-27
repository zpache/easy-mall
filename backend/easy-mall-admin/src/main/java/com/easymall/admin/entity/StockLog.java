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
