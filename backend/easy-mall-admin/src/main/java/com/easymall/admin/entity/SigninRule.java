package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 签到规则。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mk_signin_rule")
public class SigninRule extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 每日签到积分 */
    private Integer pointsPerDay;

    /** 每连续签到 N 天额外奖励，0 表示不启用 */
    private Integer bonusEveryDays;

    /** 连续签到额外奖励积分 */
    private Integer bonusPoints;

    /** 1 启用，0 停用 */
    private Integer status;

    private String remark;
}
