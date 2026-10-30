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

    /** 使用的优惠券模板 id，未用券为 null */
    private Long couponId;

    /** 优惠券抵扣金额，未用券为 0 */
    private BigDecimal couponAmount;

    /** 会员等级折扣优惠金额，未享受为 0 */
    private BigDecimal memberDiscount;

    /** 下单时的会员等级名称快照 */
    private String memberLevelName;

    /** 应付金额 */
    private BigDecimal payAmount;

    /**
     * 支付方式：0 模拟支付，1 微信支付，2 支付宝（预留）。
     * 退款按此路由渠道；存量已支付订单为 null，按当前支付配置兼容处理。
     */
    private Integer payType;

    /** 渠道交易号（微信 transaction_id 等），模拟支付为空 */
    private String payChannelNo;

    /** 已退款金额累计（支持部分退款多次累加） */
    private BigDecimal refundedAmount;

    /** 是否已发放会员积分：0 未发放，1 已发放 */
    private Integer pointsAwarded;

    /** 本单发放的积分数量 */
    private Integer pointsAmount;

    /** 已因退款回滚的积分 */
    private Integer pointsReverted;

    /** 0 待付款 / 1 待发货 / 2 待收货 / 3 已完成 / 4 已取消 */
    private Integer status;

    /** 0 无售后 / 1 退款中 / 2 已退款 / 3 售后被拒 */
    private Integer refundStatus;

    /** 支付方式：0 模拟支付 / 1 微信支付 / 2 支付宝（预留） */
    public static final int PAY_TYPE_MOCK = 0;
    public static final int PAY_TYPE_WXPAY = 1;
    public static final int PAY_TYPE_ALIPAY = 2;

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
