package com.easymall.admin.modules.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 评价审核表单。
 *
 * @param status 审核结果：1 通过，2 拒绝
 * @param remark 审核备注（拒绝时填写原因，会展示给评价人）
 */
public record CommentAuditForm(
        @NotNull(message = "请选择审核结果")
        Integer status,

        @Size(max = 200, message = "审核备注不能超过 200 个字符")
        String remark
) {
}
