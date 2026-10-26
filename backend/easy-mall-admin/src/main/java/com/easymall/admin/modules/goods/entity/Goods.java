package com.easymall.admin.modules.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.easymall.admin.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品（当前为单规格模型，后续需要多规格时再引入 SKU 表）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_goods")
public class Goods extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String name;

    private String subtitle;

    /** 主图 */
    private String mainImage;

    /** 轮播图，多张用英文逗号分隔 */
    private String images;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer sales;

    private String unit;

    /** 富文本详情 */
    private String detail;

    /** 1 上架，0 下架 */
    private Integer status;

    private Integer sort;
}
