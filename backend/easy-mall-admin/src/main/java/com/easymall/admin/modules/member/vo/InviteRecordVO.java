package com.easymall.admin.modules.member.vo;

import java.time.LocalDateTime;

/**
 * 邀请记录 VO：附被邀请人与邀请人昵称。
 */
public record InviteRecordVO(
        Long id,
        Long memberId,
        String memberNickname,
        Long inviterId,
        String inviterNickname,
        /** 0 已注册未下单 1 已完成首单 */
        Integer status,
        Integer registerPoints,
        Integer orderPoints,
        LocalDateTime createdAt,
        LocalDateTime orderedAt
) {
}
