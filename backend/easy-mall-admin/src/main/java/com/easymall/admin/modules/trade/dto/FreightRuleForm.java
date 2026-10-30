package com.easymall.admin.modules.trade.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 区域运费规则新增 / 编辑表单。
 */
public record FreightRuleForm(
        @NotBlank(message = "请选择地区")
        String regionCode,

        /** 1 提供配送 0 不配送 */
        @NotNull(message = "请选择是否配送")
        Integer deliverable,

        /** 固定运费，0 表示无条件包邮；不配送时忽略 */
        @DecimalMin(value = "0", message = "运费不能小于 0")
        BigDecimal fee,

        /** 满额包邮门槛，null 或 0 表示不启用；不配送时忽略 */
        @DecimalMin(value = "0", message = "满额包邮门槛不能小于 0")
        BigDecimal freeAmount,

        /** 乐观锁版本号，编辑时携带 */
        Integer version
) {
}
