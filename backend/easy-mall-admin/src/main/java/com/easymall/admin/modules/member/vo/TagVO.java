package com.easymall.admin.modules.member.vo;

import java.time.LocalDateTime;

/**
 * 会员标签 VO：附会员数（分群规模）。
 */
public record TagVO(
        Long id,
        String name,
        String color,
        String remark,
        Long memberCount,
        LocalDateTime createdAt
) {
}
