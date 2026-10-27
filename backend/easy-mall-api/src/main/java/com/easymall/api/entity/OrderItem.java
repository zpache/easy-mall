package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单明细（下单时快照商品信息，避免商品改价影响历史订单）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_item")
public class OrderItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long goodsId;

    private String goodsName;

    private String goodsImage;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal totalAmount;
}
