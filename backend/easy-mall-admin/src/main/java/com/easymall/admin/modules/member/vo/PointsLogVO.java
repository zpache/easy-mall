package com.easymall.admin.modules.member.vo;

import java.time.LocalDateTime;

/**
 * 积分流水 VO：附会员昵称。
 */
public record PointsLogVO(
        Long id,
        Long userId,
        String userNickname,
        Integer change,
        Integer balance,
        String type,
        String orderNo,
        String remark,
        LocalDateTime createdAt
) {
}
