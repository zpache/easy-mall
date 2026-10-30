package com.easymall.admin.modules.order.vo;

import com.easymall.admin.entity.Order;
import com.easymall.admin.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表 / 详情统一视图：带上买家昵称与商品明细，列表直接展示「首件商品 + 等 N 件」。
 */
public record OrderVO(
        Long id,
        String orderNo,
        Long userId,
        String userNickname,
        String userAvatar,
        BigDecimal totalAmount,
        BigDecimal freightAmount,
        BigDecimal couponAmount,
        BigDecimal memberDiscount,
        String memberLevelName,
        BigDecimal payAmount,
        /** 支付方式：0 模拟支付 / 1 微信支付 / 2 支付宝（预留）；存量已支付订单为 null */
        Integer payType,
        /** 渠道交易号（微信 transaction_id 等），模拟支付为空 */
        String payChannelNo,
        BigDecimal refundedAmount,
        Integer status,
        Integer refundStatus,
        String receiverName,
        String receiverPhone,
        String receiverAddress,
        String deliveryCompany,
        String deliveryNo,
        String remark,
        LocalDateTime payTime,
        LocalDateTime deliveryTime,
        LocalDateTime finishTime,
        LocalDateTime cancelTime,
        String cancelReason,
        LocalDateTime createdAt,
        int itemCount,
        int totalQuantity,
        List<OrderItem> items
) {

    public static OrderVO of(Order order, String userNickname, String userAvatar, List<OrderItem> items) {
        List<OrderItem> safeItems = items == null ? List.of() : items;
        int totalQuantity = safeItems.stream()
                .map(OrderItem::getQuantity)
                .filter(quantity -> quantity != null)
                .mapToInt(Integer::intValue)
                .sum();
        return new OrderVO(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                userNickname,
                userAvatar,
                order.getTotalAmount(),
                order.getFreightAmount(),
                order.getCouponAmount(),
                order.getMemberDiscount(),
                order.getMemberLevelName(),
                order.getPayAmount(),
                order.getPayType(),
                order.getPayChannelNo(),
                order.getRefundedAmount(),
                order.getStatus(),
                order.getRefundStatus(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getDeliveryCompany(),
                order.getDeliveryNo(),
                order.getRemark(),
                order.getPayTime(),
                order.getDeliveryTime(),
                order.getFinishTime(),
                order.getCancelTime(),
                order.getCancelReason(),
                order.getCreatedAt(),
                safeItems.size(),
                totalQuantity,
                safeItems);
    }
}
