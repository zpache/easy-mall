package com.easymall.admin.modules.member.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员列表 / 详情 VO：聚合等级名称与标签。
 */
public record MemberVO(
        Long id,
        String nickname,
        String avatar,
        String phone,
        Integer gender,
        Integer status,
        Long levelId,
        String levelName,
        BigDecimal discount,
        Integer points,
        Integer source,
        BigDecimal totalSpend,
        Integer orderCount,
        List<TagItem> tags,
        LocalDateTime createdAt
) {

    /**
     * 会员标签项。
     */
    public record TagItem(Long id, String name, String color) {
    }
}
