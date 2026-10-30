package com.easymall.admin.modules.order.vo;

import com.easymall.admin.entity.Order;
import com.easymall.admin.entity.OrderRefund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后列表视图：附带订单状态与下单时间，方便客服判断上下文。
 */
public record RefundVO(
        Long id,
        String refundNo,
        /** 微信支付退款单号，模拟退款为 null */
        String wxRefundNo,
        Long orderId,
        String orderNo,
        Long userId,
        String userNickname,
        BigDecimal amount,
        String type,
        String reason,
        Integer status,
        String auditRemark,
        LocalDateTime appliedAt,
        LocalDateTime handledAt,
        Integer orderStatus,
        LocalDateTime orderCreatedAt
) {

    public static RefundVO of(OrderRefund refund, Order order, String userNickname) {
        return new RefundVO(
                refund.getId(),
                refund.getRefundNo(),
                refund.getWxRefundNo(),
                refund.getOrderId(),
                refund.getOrderNo(),
                refund.getUserId(),
                userNickname,
                refund.getAmount(),
                refund.getType(),
                refund.getReason(),
                refund.getStatus(),
                refund.getAuditRemark(),
                refund.getAppliedAt(),
                refund.getHandledAt(),
                order == null ? null : order.getStatus(),
                order == null ? null : order.getCreatedAt());
    }
}
