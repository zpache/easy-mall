package com.easymall.api.modules.order.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后申请结果。
 */
public record RefundApplyVO(
        Long id,
        String refundNo,
        BigDecimal amount,
        Integer status,
        String statusText,
        LocalDateTime appliedAt
) {

    public static String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待商家审核";
            case 1 -> "商家已同意，等待退款";
            case 2 -> "商家已拒绝";
            case 3 -> "已退款";
            default -> "未知";
        };
    }
}
