package com.easymall.admin.modules.marketing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 签到规则新增 / 编辑表单。
 */
public record SigninRuleForm(
        @NotBlank(message = "规则名称不能为空")
        @Size(max = 100, message = "规则名称不能超过 100 个字符")
        String name,

        @NotNull(message = "每日签到积分不能为空")
        @Min(value = 1, message = "每日签到积分至少为 1")
        @Max(value = 1000, message = "每日签到积分不能超过 1000")
        Integer pointsPerDay,

        @Min(value = 0, message = "连续签到天数不能为负数")
        @Max(value = 365, message = "连续签到天数不能超过 365")
        Integer bonusEveryDays,

        @Min(value = 0, message = "额外奖励积分不能为负数")
        @Max(value = 10000, message = "额外奖励积分不能超过 10000")
        Integer bonusPoints,

        Integer status,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark,

        /** 乐观锁版本号，编辑时携带 */
        Integer version
) {
}
