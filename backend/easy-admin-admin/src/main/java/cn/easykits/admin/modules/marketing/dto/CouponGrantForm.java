package cn.easykits.admin.modules.marketing.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 优惠券定向发放表单：把券直接发放到指定会员账户（免领取）。
 */
public record CouponGrantForm(
        @NotEmpty(message = "请选择要发放的会员")
        List<Long> userIds
) {
}
