package com.easymall.admin.modules.trade.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 运费配置表单（全局单行）。
 */
public record FreightConfigForm(
        @NotNull(message = "统一运费不能为空")
        @DecimalMin(value = "0", message = "统一运费不能小于 0")
        BigDecimal defaultFee,

        /** 满额包邮门槛，null 或 0 表示不启用 */
        @DecimalMin(value = "0", message = "满额包邮门槛不能小于 0")
        BigDecimal freeAmount,

        /** 乐观锁版本号 */
        Integer version
) {
}
