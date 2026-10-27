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
 * 拼团秒杀活动新增 / 编辑表单。
 */
public record PromotionForm(
        @NotBlank(message = "活动名称不能为空")
        @Size(max = 100, message = "活动名称不能超过 100 个字符")
        String name,

        @NotNull(message = "活动类型不能为空")
        @Min(value = 1, message = "活动类型只能为 1 秒杀 / 2 拼团")
        @Max(value = 2, message = "活动类型只能为 1 秒杀 / 2 拼团")
        Integer type,

        @NotNull(message = "商品不能为空")
        Long goodsId,

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 200, message = "商品名称不能超过 200 个字符")
        String goodsName,

        @NotNull(message = "原价不能为空")
        @DecimalMin(value = "0.01", message = "原价必须大于 0")
        BigDecimal originalPrice,

        @NotNull(message = "活动价不能为空")
        @DecimalMin(value = "0.01", message = "活动价必须大于 0")
        BigDecimal promoPrice,

        @Min(value = 2, message = "成团人数至少为 2")
        Integer groupSize,

        @Min(value = 1, message = "成团有效时长至少为 1 小时")
        Integer groupHours,

        @NotNull(message = "活动库存不能为空")
        @Min(value = 1, message = "活动库存至少为 1")
        Integer stock,

        @NotNull(message = "开始时间不能为空")
        LocalDateTime startTime,

        @NotNull(message = "结束时间不能为空")
        LocalDateTime endTime,

        Integer status,

        /** 乐观锁版本号，编辑时携带 */
        Integer version
) {
}
