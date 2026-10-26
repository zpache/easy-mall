package com.easymall.admin.modules.goods.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 商品新增/编辑表单。
 */
public record GoodsForm(
        @NotNull(message = "请选择商品分类")
        Long categoryId,

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 100, message = "商品名称过长")
        String name,

        @Size(max = 200, message = "副标题过长")
        String subtitle,

        @NotBlank(message = "请上传商品主图")
        String mainImage,

        String images,

        @NotNull(message = "销售价不能为空")
        @DecimalMin(value = "0.00", message = "销售价不能小于 0")
        BigDecimal price,

        @DecimalMin(value = "0.00", message = "划线价不能小于 0")
        BigDecimal originalPrice,

        @NotNull(message = "库存不能为空")
        @Min(value = 0, message = "库存不能小于 0")
        Integer stock,

        String unit,

        String detail,

        @NotNull(message = "状态不能为空")
        Integer status,

        Integer sort,

        /** 乐观锁版本号：编辑时原样带上详情返回的 version，新增留空 */
        @Min(value = 0, message = "版本号不合法")
        Integer version
) {
}
