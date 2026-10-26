package com.easymall.api.modules.order.vo;

import com.easymall.api.modules.order.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图。
 */
public record OrderVO(
        Long id,
        String orderNo,
        Integer status,
        String statusText,
        BigDecimal totalAmount,
        BigDecimal freightAmount,
        BigDecimal payAmount,
        String receiverName,
        String receiverPhone,
        String receiverAddress,
        String remark,
        LocalDateTime createdAt,
        List<OrderItem> items
) {
    public static String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待付款";
            case 1 -> "待发货";
            case 2 -> "待收货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }
}
