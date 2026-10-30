package com.easymall.admin.modules.member.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 会员等级折扣总开关表单。
 *
 * <p>全局只有一个开关，存放在 sys_config 的 member 分组下，
 * 与小程序端 MemberDiscountService 读取同一份配置。</p>
 */
public record LevelDiscountSwitchForm(

        /** 是否开启会员等级折扣 */
        @NotNull(message = "请选择是否开启等级折扣")
        Boolean enabled
) {
}
