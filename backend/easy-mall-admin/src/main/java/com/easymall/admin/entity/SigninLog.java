package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员签到记录：一人一天一条，留痕积分发放。
 */
@Data
@TableName("mk_signin_log")
public class SigninLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate signDate;

    private Integer continuousDays;

    /** 本次获得积分 */
    private Integer points;

    private LocalDateTime createdAt;
}
