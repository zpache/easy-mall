package com.easymall.admin.modules.marketing.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 签到记录视图：补齐会员昵称。
 */
public record SigninLogVO(
        Long id,
        Long userId,
        String nickname,
        LocalDate signDate,
        Integer continuousDays,
        Integer points,
        LocalDateTime createdAt
) {
}
