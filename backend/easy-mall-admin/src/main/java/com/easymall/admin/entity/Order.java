package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表（商城端下单产生，管理端负责发货、售后与评价）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("o_order")
public class Order extends BaseEntity {

    /** 0 待付款 */
    public static final int STATUS_UNPAID = 0;
    /** 1 待发货 */
    public static final int STATUS_UNDELIVERED = 1;
    /** 2 待收货 */
    public static final int STATUS_UNRECEIVED = 2;
    /** 3 已完成 */
    public static final int STATUS_FINISHED = 3;
    /** 4 已取消 */
    public static final int STATUS_CANCELLED = 4;

    /** 无售后 */
    public static final int REFUND_NONE = 0;
    /** 退款中 */
    public static final int REFUND_PROCESSING = 1;
    /** 已退款 */
    public static final int REFUND_DONE = 2;
    /** 售后被拒 */
    public static final int REFUND_REJECTED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    /** 商品总额 */
    private BigDecimal totalAmount;

    private BigDecimal freightAmount;

    /** 应付金额 */
    private BigDecimal payAmount;

    /** 0 待付款 / 1 待发货 / 2 待收货 / 3 已完成 / 4 已取消 */
    private Integer status;

    /** 0 无售后 / 1 退款中 / 2 已退款 / 3 售后被拒 */
    private Integer refundStatus;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String deliveryCompany;

    private String deliveryNo;

    private String remark;

    private LocalDateTime payTime;

    private LocalDateTime deliveryTime;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    private String cancelReason;
}
