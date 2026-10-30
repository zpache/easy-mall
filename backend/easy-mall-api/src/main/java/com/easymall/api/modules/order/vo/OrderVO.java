package com.easymall.api.modules.order.vo;

import com.easymall.api.entity.OrderItem;

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
        /** 售后状态：0 无 / 1 退款中 / 2 已退款 / 3 售后被拒 */
        Integer refundStatus,
        String refundStatusText,
        BigDecimal totalAmount,
        BigDecimal freightAmount,
        BigDecimal couponAmount,
        /** 会员等级折扣优惠金额，未享受折扣为 0 */
        BigDecimal memberDiscount,
        /** 享受折扣时的会员等级名称（下单时快照） */
        String memberLevelName,
        BigDecimal payAmount,
        String receiverName,
        String receiverPhone,
        String receiverAddress,
        /** 快递公司，未发货为 null */
        String deliveryCompany,
        /** 快递单号，未发货为 null */
        String deliveryNo,
        /** 发货时间，未发货为 null */
        LocalDateTime deliveryTime,
        String remark,
        LocalDateTime createdAt,
        /** 支付截止时间：仅待付款订单有值，超时后订单自动关闭 */
        LocalDateTime payDeadline,
        /** 距离支付截止还剩多少秒：由服务端计算，规避客户端时钟偏差 */
        Long payRemainSeconds,
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

    /** 售后状态文案：0 无售后时不展示，返回 null */
    public static String refundStatusText(Integer refundStatus) {
        if (refundStatus == null || refundStatus == 0) {
            return null;
        }
        return switch (refundStatus) {
            case 1 -> "退款处理中";
            case 2 -> "已退款";
            case 3 -> "售后被拒绝";
            default -> "未知";
        };
    }
}
