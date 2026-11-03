package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邀请关系：小程序分享带 inviter 进入，新用户注册时落库（只写，不做逻辑删除）。
 */
@Data
@TableName("m_member_invite")
public class MemberInvite implements Serializable {

    /** 已注册未下单 */
    public static final int STATUS_REGISTERED = 0;
    /** 已完成首单 */
    public static final int STATUS_ORDERED = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被邀请人（新用户） */
    private Long memberId;

    /** 邀请人 */
    private Long inviterId;

    /** 0 已注册未下单 1 已完成首单 */
    private Integer status;

    /** 注册成功时奖励邀请人的积分 */
    private Integer registerPoints;

    /** 首单支付成功时奖励邀请人的积分 */
    private Integer orderPoints;

    /** 首单支付完成时间 */
    private LocalDateTime orderedAt;

    /** 建立关系时间 */
    private LocalDateTime createdAt;
}
