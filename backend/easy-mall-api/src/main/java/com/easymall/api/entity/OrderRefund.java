package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后退款单（小程序发起，后台审核与打款）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("o_order_refund")
public class OrderRefund extends BaseEntity {

    /** 待审核 */
    public static final int STATUS_PENDING = 0;
    /** 已同意，等待退款 */
    public static final int STATUS_APPROVED = 1;
    /** 已拒绝 */
    public static final int STATUS_REJECTED = 2;
    /** 已退款 */
    public static final int STATUS_REFUNDED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private BigDecimal amount;

    /** refund 仅退款 / return 退货退款 */
    private String type;

    private String reason;

    private Integer status;

    private String auditRemark;

    private LocalDateTime appliedAt;

    private LocalDateTime handledAt;

    private Long handledBy;

    /** 微信支付退款单号，模拟退款为空 */
    private String wxRefundNo;
}
