package com.easymall.admin.modules.order.dto;

import jakarta.validation.constraints.Size;

/**
 * 售后审核表单。
 *
 * @param pass   是否同意退款
 * @param remark 审核意见（拒绝时建议填写原因）
 */
public record RefundAuditForm(
        Boolean pass,

        @Size(max = 255, message = "审核意见不能超过 255 个字符")
        String remark
) {
}
