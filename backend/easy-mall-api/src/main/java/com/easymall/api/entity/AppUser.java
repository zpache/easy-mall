package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 小程序用户（微信登录后自动创建）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("m_member")
public class AppUser extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String openid;

    private String unionid;

    private String nickname;

    private String avatar;

    private String phone;

    /** 0 未知，1 男，2 女 */
    private Integer gender;

    /**
     * 会员等级 ID。
     * 等级、积分、累计消费由交易链路（MemberPointService）维护，
     * 这里的 updateStrategy = NEVER 保证 updateById 改资料时不会用旧值覆盖。
     */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long levelId;

    /** 当前积分（消费获得，管理端也可人工调整） */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer points;

    /** 累计消费金额（已付款订单实付合计） */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private java.math.BigDecimal totalSpend;

    /** 累计订单数（已付款订单） */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer orderCount;

    /** 1 正常，0 禁用 */
    private Integer status;

    /** 等级名称，仅用于展示，不落库 */
    @TableField(exist = false)
    private String levelName;

    /** 等级折扣百分比（100 表示无折扣），仅用于展示，不落库 */
    @TableField(exist = false)
    private java.math.BigDecimal levelDiscount;
}
