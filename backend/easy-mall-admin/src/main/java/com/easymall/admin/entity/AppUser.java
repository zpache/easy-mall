package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 小程序买家账号（订单、评价归属主体）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_user")
public class AppUser extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String openid;

    private String unionid;

    private String nickname;

    private String avatar;

    private String phone;

    private Integer gender;

    /** 会员等级 ID，默认 1（普通会员） */
    private Long levelId;

    /** 当前积分 */
    private Integer points;

    /** 来源渠道：0 公众号 / 1 小程序 / 2 H5 扫码 */
    private Integer source;

    /** 累计消费金额（已付款订单） */
    private java.math.BigDecimal totalSpend;

    /** 累计订单数（已付款订单） */
    private Integer orderCount;

    /** 1 正常，0 禁用 */
    private Integer status;
}
