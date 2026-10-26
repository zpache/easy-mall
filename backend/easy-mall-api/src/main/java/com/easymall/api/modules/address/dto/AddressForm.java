package com.easymall.api.modules.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 收货地址表单。
 */
public record AddressForm(
        @NotBlank(message = "收货人不能为空") @Size(max = 32, message = "收货人过长") String receiver,
        @NotBlank(message = "手机号不能为空") @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        @NotBlank(message = "请选择省份") String province,
        @NotBlank(message = "请选择城市") String city,
        @NotBlank(message = "请选择区县") String district,
        @NotBlank(message = "请填写详细地址") @Size(max = 255, message = "详细地址过长") String detail,
        Integer isDefault
) {
}
