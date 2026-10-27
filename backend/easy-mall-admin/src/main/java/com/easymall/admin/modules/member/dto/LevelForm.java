package com.easymall.admin.modules.member.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 会员等级新增 / 编辑表单。
 */
public record LevelForm(
        @NotBlank(message = "等级名称不能为空")
        @Size(max = 50, message = "等级名称不能超过 50 个字符")
        String name,

        @NotNull(message = "折扣不能为空")
        @DecimalMin(value = "50.0", message = "折扣不能低于 50")
        @DecimalMax(value = "100.0", message = "折扣不能超过 100")
        BigDecimal discount,

        @NotNull(message = "升级门槛不能为空")
        @Min(value = 0, message = "升级门槛不能为负数")
        Integer threshold,

        @Size(max = 500, message = "权益说明不能超过 500 个字符")
        String description,

        Integer sort,

        Integer status,

        /** 乐观锁版本号，编辑时携带 */
        Integer version
) {
}
