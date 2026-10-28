package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员领券记录（只读）。
 */
@Data
@TableName("mk_coupon_user")
public class CouponUser implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long couponId;

    private Long userId;

    /** 0 未使用 1 已使用 2 已过期 */
    private Integer status;

    /** 核销订单号 */
    private String orderNo;

    private LocalDateTime receivedAt;

    private LocalDateTime usedAt;

    private LocalDateTime expiredAt;
}
