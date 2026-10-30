package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品。
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

    private String mainImage;

    private String images;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    /** 真实销量：下单累加、取消回滚 */
    private Integer sales;

    /** 基础销量：运营维护的展示基数，不对外暴露，返回前并入 sales */
    @JsonIgnore
    private Integer baseSales;

    private String unit;

    private String detail;

    private Integer status;

    private Integer sort;
}
