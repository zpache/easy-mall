package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团秒杀活动。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mk_promotion")
public class Promotion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 1 秒杀 2 拼团 */
    private Integer type;

    private Long goodsId;

    /** 商品名称快照 */
    private String goodsName;

    private BigDecimal originalPrice;

    /** 活动价（秒杀价 / 拼团价） */
    private BigDecimal promoPrice;

    /** 成团人数（拼团） */
    private Integer groupSize;

    /** 成团有效时长（小时，拼团） */
    private Integer groupHours;

    private Integer stock;

    private Integer soldCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 1 启用，0 停用 */
    private Integer status;
}
