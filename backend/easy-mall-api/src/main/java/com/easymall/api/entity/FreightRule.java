package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 区域运费规则（省 / 市两级）。只读。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_freight_rule")
public class FreightRule extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String regionCode;

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
