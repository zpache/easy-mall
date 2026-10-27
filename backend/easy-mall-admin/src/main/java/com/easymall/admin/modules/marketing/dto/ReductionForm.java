package com.easymall.admin.modules.marketing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 满减活动新增 / 编辑表单。
 */
public record ReductionForm(
        @NotBlank(message = "活动名称不能为空")
        @Size(max = 100, message = "活动名称不能超过 100 个字符")
        String name,

        @NotNull(message = "满减模式不能为空")
        @Min(value = 1, message = "满减模式只能为 1 阶梯满减 / 2 每满减")
        @Max(value = 2, message = "满减模式只能为 1 阶梯满减 / 2 每满减")
        Integer mode,

        @NotNull(message = "满减门槛不能为空")
        @DecimalMin(value = "0.01", message = "满减门槛必须大于 0")
        BigDecimal threshold,

        @NotNull(message = "满减金额不能为空")
        @DecimalMin(value = "0.01", message = "满减金额必须大于 0")
        BigDecimal benefit,

        @NotNull(message = "开始时间不能为空")
        LocalDateTime startTime,

        @NotNull(message = "结束时间不能为空")
        LocalDateTime endTime,

        Integer status,

        /** 乐观锁版本号，编辑时携带 */
        Integer version
) {
}
