package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("o_order")
public class MallOrder extends BaseEntity {

    /** 订单状态：0 待付款 / 1 待发货 / 2 待收货 / 3 已完成 / 4 已取消 */
    public static final int STATUS_UNPAID = 0;
    public static final int STATUS_UNDELIVERED = 1;
    public static final int STATUS_UNRECEIVED = 2;
    public static final int STATUS_FINISHED = 3;
    public static final int STATUS_CANCELLED = 4;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private BigDecimal freightAmount;

    /** 使用的优惠券模板 id，未用券为 null */
    private Long couponId;

    /** 优惠券抵扣金额，未用券为 0 */
    private BigDecimal couponAmount;

    /** 会员等级折扣优惠金额（下单时的等级折扣，未享受为 0） */
    private BigDecimal memberDiscount;

    /** 下单时的会员等级名称快照，未享受折扣为 null */
    private String memberLevelName;

    private BigDecimal payAmount;

    /** 已退款金额累计（支持部分退款多次累加） */
    private BigDecimal refundedAmount;

    private Integer status;

    /** 0 无售后 / 1 退款中 / 2 已退款 / 3 售后被拒（与 o_order.refund_status 口径一致） */
    private Integer refundStatus;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;

    private LocalDateTime payTime;

    /** 发货快递公司（后台发货时写入） */
    private String deliveryCompany;

    /** 发货快递单号 */
    private String deliveryNo;

    private LocalDateTime deliveryTime;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    /** 是否已发放会员积分：0 未发放，1 已发放（幂等标记） */
    private Integer pointsAwarded;

    /** 本单发放的积分数量 */
    private Integer pointsAmount;

    /** 已因退款回滚的积分（避免重复退款重复扣减） */
    private Integer pointsReverted;
}
