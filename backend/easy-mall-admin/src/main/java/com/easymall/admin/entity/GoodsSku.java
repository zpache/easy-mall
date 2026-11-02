package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品 SKU：一个规格组合一行，价格与库存以 SKU 为准。
 * 单规格商品（mall_goods.specs 为空）不会产生 SKU。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_goods_sku")
public class GoodsSku extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long goodsId;

    /** SKU 编码：商品编码 + 两位序号，保存时系统生成 */
    private String skuCode;

    /** 规格值文案：按规格顺序逗号拼接，如 红色,S */
    private String specText;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer sales;

    /** 规格图，留空回落到商品主图 */
    private String image;

    /** 1 启用 0 停用 */
    private Integer status;
}
