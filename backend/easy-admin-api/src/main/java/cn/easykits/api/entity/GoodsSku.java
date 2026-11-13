package cn.easykits.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品 SKU：一个规格组合一行，价格与库存以 SKU 为准。
 * 单规格商品（无规格定义）不会产生 SKU，直接使用商品表的价格与库存。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_goods_sku")
public class GoodsSku extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long goodsId;

    /** SKU 编码：商品编码 + 两位序号 */
    private String skuCode;

    /** 规格值文案：按规格顺序逗号拼接，如 红色,S */
    private String specText;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    /** 该 SKU 的真实销量 */
    private Integer sales;

    /** 规格图，留空时前端回落到商品主图 */
    private String image;

    /** 1 启用 0 停用 */
    private Integer status;
}
