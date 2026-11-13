package cn.easykits.admin.entity;

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

    /** 商品编码：新增时系统生成的 10 位字母数字唯一编码，生成后不可修改 */
    private String goodsCode;

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

    /** 真实销量：用户下单累加、取消回滚，管理后台以此为准 */
    private Integer sales;

    /** 基础销量：运营维护的展示基数，小程序展示销量 = 基础销量 + 真实销量 */
    private Integer baseSales;

    private String unit;

    /**
     * 规格定义 JSON：[{"name":"颜色","values":["红","蓝"]}]。
     * 为空表示单规格商品；有值时价格与库存由 mall_goods_sku 汇总而来。
     */
    private String specs;

    /** 富文本详情 */
    private String detail;

    /** 1 上架，0 下架 */
    private Integer status;

    private Integer sort;
}
