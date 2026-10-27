package com.easymall.admin.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 订单发货表单。
 */
public record DeliverForm(
        @NotBlank(message = "请填写快递公司")
        @Size(max = 50, message = "快递公司不能超过 50 个字符")
        String deliveryCompany,

        @NotBlank(message = "请填写快递单号")
        @Size(max = 64, message = "快递单号不能超过 64 个字符")
        String deliveryNo
) {
}
