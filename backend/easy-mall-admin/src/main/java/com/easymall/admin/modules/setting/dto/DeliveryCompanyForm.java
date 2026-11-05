package com.easymall.admin.modules.setting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 快递公司新增 / 编辑表单。
 *
 * @param name          展示名，发货时写入订单
 * @param code          快递100 公司编码
 * @param phoneRequired 查询轨迹是否必须传手机号后四位
 * @param sort          排序，越小越靠前
 * @param status        1 启用 0 停用
 */
public record DeliveryCompanyForm(
        @NotBlank(message = "请填写快递公司名称")
        @Size(max = 50, message = "名称不能超过 50 个字符")
        String name,

        @NotBlank(message = "请填写快递100 公司编码")
        @Size(max = 32, message = "编码不能超过 32 个字符")
        String code,

        Boolean phoneRequired,

        Integer sort,

        Integer status,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark
) {
}
