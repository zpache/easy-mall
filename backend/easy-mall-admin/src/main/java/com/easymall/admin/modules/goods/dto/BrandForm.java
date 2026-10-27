package com.easymall.admin.modules.goods.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 品牌新增 / 编辑表单。
 */
public record BrandForm(
        @NotBlank(message = "品牌名称不能为空")
        @Size(max = 50, message = "品牌名称过长")
        String name,

        @Size(max = 255, message = "LOGO 地址过长")
        String logo,

        Integer sort,

        @NotNull(message = "状态不能为空")
        Integer status,

        @Size(max = 255, message = "备注过长")
        String remark,

        /** 乐观锁版本号：编辑时原样带上详情返回的 version，新增留空 */
        @Min(value = 0, message = "版本号不合法")
        Integer version
) {
}
