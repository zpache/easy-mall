package com.easymall.admin.modules.goods.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 商品分类表单。
 */
public record CategoryForm(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 32, message = "分类名称过长")
        String name,

        @NotNull(message = "上级分类不能为空")
        Long parentId,

        @Size(max = 255, message = "图标地址过长")
        String icon,

        Integer sort,

        @NotNull(message = "状态不能为空")
        Integer status,

        /** 乐观锁版本号：编辑时原样带上详情返回的 version，新增留空 */
        @Min(value = 0, message = "版本号不合法")
        Integer version
) {
}
