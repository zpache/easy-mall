package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 区域运费规则（省 / 市两级）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_freight_rule")
public class FreightRule extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 行政区域编码（region.area_code） */
    private String regionCode;

    /** 行政区域名称（冗余，便于展示） */
    private String regionName;

    /** 1 省 2 市 */
    private Integer regionLevel;

    /** 1 提供配送 0 不配送 */
    private Integer deliverable;

    /** 固定运费，0 表示该地区无条件包邮 */
    private BigDecimal fee;

    /** 满额包邮门槛，NULL 表示不启用 */
    private BigDecimal freeAmount;
}
