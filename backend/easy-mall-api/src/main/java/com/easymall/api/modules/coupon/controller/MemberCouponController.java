package com.easymall.api.modules.coupon.controller;

import com.easymall.api.common.api.Result;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.modules.coupon.service.MemberCouponService;
import com.easymall.api.modules.coupon.vo.MemberCouponResult;
import com.easymall.api.modules.coupon.vo.MemberCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我的优惠券。需要登录态（不在免登录白名单内）。
 */
@Tag(name = "优惠券")
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class MemberCouponController {

    private final MemberCouponService memberCouponService;

    @Operation(summary = "我的优惠券（含各状态数量）")
    @GetMapping("/mine")
    public Result<MemberCouponResult> mine(@RequestParam(required = false) Integer status) {
        if (status != null && (status < 0 || status > 2)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能是 0/1/2");
        }
        return Result.ok(memberCouponService.mine(status));
    }

    @Operation(summary = "领券中心：领取一张优惠券")
    @PostMapping("/{id}/receive")
    public Result<MemberCouponVO> receive(@PathVariable Long id) {
        return Result.ok(memberCouponService.receive(id));
    }
}
