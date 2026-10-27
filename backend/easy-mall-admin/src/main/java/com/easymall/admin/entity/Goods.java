package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    /** 品牌 ID，可为空 */
    private Long brandId;

    private String name;

    private String subtitle;

    /** 主图 */
    private String mainImage;

    /** 轮播图，多张用英文逗号分隔 */
    private String images;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    /** 库存预警值：库存低于等于该值时在库存管理中标注 */
    private Integer stockWarn;

    private Integer sales;

    private String unit;

    /** 富文本详情 */
    private String detail;

    /** 1 上架，0 下架 */
    private Integer status;

    private Integer sort;
}
