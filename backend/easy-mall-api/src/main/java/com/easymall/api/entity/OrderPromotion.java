package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单优惠快照：订单与参加的每个满减活动 1 对多。
 * 下单时落活动快照，活动后续修改不影响历史订单展示与退款口径。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("o_order_promotion")
public class OrderPromotion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderNo;

    /** 满减活动 ID */
    private Long promotionId;

    /** 活动名称快照 */
    private String promotionName;

    /** 1 阶梯满减 2 每满减（快照） */
    private Integer mode;

    /** 满 X 元（快照） */
    private BigDecimal threshold;

    /** 减 Y 元（快照） */
    private BigDecimal benefit;

    /** 命中次数：每满减按金额可叠加多次，阶梯为 1 */
    private Integer times;

    /** 本单该活动实际优惠金额 */
    private BigDecimal discountAmount;
}
