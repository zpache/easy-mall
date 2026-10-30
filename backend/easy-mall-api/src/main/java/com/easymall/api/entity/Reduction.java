package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 满减活动（只读，管理端维护）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mk_reduction")
public class Reduction extends BaseEntity {

    /** 阶梯满减：一记录一档，命中档里取优惠最大的一条 */
    public static final int MODE_LADDER = 1;
    /** 每满减：满 threshold 减 benefit，按金额可叠加多次 */
    public static final int MODE_EVERY = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 1 阶梯满减 2 每满减 */
    private Integer mode;

    /** 满 X 元 */
    private BigDecimal threshold;

    /** 减 Y 元 */
    private BigDecimal benefit;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 1 启用，0 停用 */
    private Integer status;
}
