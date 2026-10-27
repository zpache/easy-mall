package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 优惠券。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mk_coupon")
public class Coupon extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 1 满减券 2 折扣券 3 无门槛券 */
    private Integer type;

    /** 面额（元），满减券 / 无门槛券 */
    private BigDecimal amount;

    /** 折扣（百分比，90 表示 9 折），折扣券 */
    private BigDecimal discount;

    /** 使用门槛（元），0 表示无门槛 */
    private BigDecimal threshold;

    private Integer totalCount;

    private Integer sendCount;

    private Integer usedCount;

    /** 每人限领张数 */
    private Integer perLimit;

    /** 1 固定日期 2 领取后 N 天 */
    private Integer validType;

    private LocalDate validStart;

    private LocalDate validEnd;

    /** 领取后有效天数 */
    private Integer validDays;

    /** 1 启用，0 停用 */
    private Integer status;

    private String remark;
}
