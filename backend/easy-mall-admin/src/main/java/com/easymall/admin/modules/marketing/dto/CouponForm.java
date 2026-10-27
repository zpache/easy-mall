package com.easymall.admin.modules.marketing.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 优惠券新增 / 编辑表单。
 */
public record CouponForm(
        @NotBlank(message = "券名称不能为空")
        @Size(max = 100, message = "券名称不能超过 100 个字符")
        String name,

        @NotNull(message = "券类型不能为空")
        @Min(value = 1, message = "券类型只能为 1 满减 / 2 折扣 / 3 无门槛")
        @Max(value = 3, message = "券类型只能为 1 满减 / 2 折扣 / 3 无门槛")
        Integer type,

        @DecimalMin(value = "0.01", message = "面额必须大于 0")
        BigDecimal amount,

        @DecimalMin(value = "50.0", message = "折扣不能低于 50")
        @DecimalMax(value = "99.9", message = "折扣不能高于 99.9")
        BigDecimal discount,

        @DecimalMin(value = "0", message = "使用门槛不能为负数")
        BigDecimal threshold,

        @NotNull(message = "发放总量不能为空")
        @Min(value = 1, message = "发放总量至少为 1")
        Integer totalCount,

        @Min(value = 1, message = "每人限领至少为 1")
        Integer perLimit,

        @NotNull(message = "有效期类型不能为空")
        @Min(value = 1, message = "有效期类型只能为 1 固定日期 / 2 领取后 N 天")
        @Max(value = 2, message = "有效期类型只能为 1 固定日期 / 2 领取后 N 天")
        Integer validType,

        LocalDate validStart,

        LocalDate validEnd,

        @Min(value = 1, message = "领取后有效天数至少为 1")
        Integer validDays,

        Integer status,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark,

        /** 乐观锁版本号，编辑时携带 */
        Integer version
) {
}
