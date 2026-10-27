package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 满减活动。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mk_reduction")
public class Reduction extends BaseEntity {

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
