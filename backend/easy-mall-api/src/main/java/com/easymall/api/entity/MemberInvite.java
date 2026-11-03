package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邀请关系（只写）：新用户经分享链接注册时建立，一人只能有一条记录。
 *
 * <p>表结构没有 updated_at / version / deleted，不继承 BaseEntity。</p>
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

    private LocalDateTime createdAt;
}
