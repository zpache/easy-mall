package com.easymall.admin.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 部门新增 / 编辑表单。
 *
 * @param pid    上级部门 ID，0 或空表示根部门
 * @param name   部门名称
 * @param sort   排序值，越小越前
 * @param status 1 正常，0 禁用
 * @param remark 备注
 */
public record DeptForm(
        Long pid,

        @NotBlank(message = "部门名称不能为空")
        @Size(max = 50, message = "部门名称不能超过 50 个字符")
        String name,

        Integer sort,

        Integer status,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark
) {
}
