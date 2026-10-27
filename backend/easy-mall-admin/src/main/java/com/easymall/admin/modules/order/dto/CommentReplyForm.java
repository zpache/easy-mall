package com.easymall.admin.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 评价回复表单。
 */
public record CommentReplyForm(
        @NotBlank(message = "请填写回复内容")
        @Size(max = 500, message = "回复内容不能超过 500 个字符")
        String reply
) {
}
