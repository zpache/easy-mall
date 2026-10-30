package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 运费配置（全局单行，id 固定为 1）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_freight_config")
public class FreightConfig extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 统一运费：未命中区域规则时按此金额收取 */
    private BigDecimal defaultFee;

    /** 全国满额包邮门槛，NULL 表示不启用（仅作用于按统一运费计费的地区） */
    private BigDecimal freeAmount;

    private String remark;
}
