package cn.easykits.api.modules.member.vo;

import cn.easykits.api.entity.AppUser;
import cn.easykits.api.entity.MemberInvite;

import java.time.LocalDateTime;

/**
 * 邀请记录。
 *
 * @param id             记录 ID
 * @param nickname       被邀请人昵称
 * @param avatar         被邀请人头像
 * @param status         0 已注册未下单 1 已完成首单
 * @param registerPoints 本次带来的注册奖励积分
 * @param orderPoints    本次带来的首单奖励积分
 * @param createdAt      建立关系时间
 * @param orderedAt      首单支付完成时间
 */
public record InviteRecordVO(
        Long id,
        String nickname,
        String avatar,
        Integer status,
        Integer registerPoints,
        Integer orderPoints,
        LocalDateTime createdAt,
        LocalDateTime orderedAt
) {

    public static InviteRecordVO of(MemberInvite invite, AppUser member) {
        return new InviteRecordVO(
                invite.getId(),
                member == null ? "" : member.getNickname(),
                member == null ? "" : (member.getAvatar() == null ? "" : member.getAvatar()),
                invite.getStatus(),
                invite.getRegisterPoints(),
                invite.getOrderPoints(),
                invite.getCreatedAt(),
                invite.getOrderedAt()
        );
    }
}
