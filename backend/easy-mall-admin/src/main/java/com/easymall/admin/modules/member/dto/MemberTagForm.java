package com.easymall.admin.modules.member.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 会员打标签表单：全量覆盖该会员的标签。
 */
public record MemberTagForm(
        @NotNull(message = "标签 ID 列表不能为空")
        List<Long> tagIds
) {
}
