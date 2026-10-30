package com.easymall.api.modules.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 售后申请：金额由服务端按「实付 − 已退」计算，不接受前端传值，
 * 避免用户申请超额退款。
 */
public record RefundApplyForm(

        @NotNull(message = "订单 ID 不能为空")
        Long orderId,

        /** refund 仅退款 / return 退货退款，缺省按仅退款 */
        String type,

        @Size(max = 200, message = "申请原因不能超过 200 字")
        String reason
) {
}
