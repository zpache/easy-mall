package cn.easykits.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

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

    /**
     * 规格定义 JSON：[{"name":"颜色","values":["红","蓝"]}]。
     * 为 null 表示单规格商品，直接使用商品表的价格与库存。
     */
    private String specs;

    private String detail;

    private Integer status;

    private Integer sort;

    /** SKU 列表：仅商品详情返回，列表接口不填充 */
    @TableField(exist = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<GoodsSku> skus;
}
