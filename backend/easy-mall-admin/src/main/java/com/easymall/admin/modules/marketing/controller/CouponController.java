package com.easymall.admin.modules.marketing.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.Coupon;
import com.easymall.admin.modules.marketing.dto.CouponForm;
import com.easymall.admin.modules.marketing.service.CouponService;
import com.easymall.admin.modules.marketing.vo.CouponUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券：券模板维护 + 领券记录查询。
 */
@Tag(name = "营销-优惠券")
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "分页查询优惠券")
    @GetMapping
    public Result<PageResult<Coupon>> page(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer type,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(couponService.pageQuery(pageNum, pageSize, keyword, type, status));
    }

    @Operation(summary = "领券记录分页")
    @GetMapping("/records")
    public Result<PageResult<CouponUserVO>> records(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) Long couponId,
                                                    @RequestParam(required = false) Long userId,
                                                    @RequestParam(required = false) Integer status) {
        return Result.ok(couponService.pageRecords(pageNum, pageSize, couponId, userId, status));
    }

    @Operation(summary = "新增优惠券")
    @PostMapping
    public Result<Coupon> create(@Valid @RequestBody CouponForm form) {
        return Result.ok(couponService.create(form));
    }

    @Operation(summary = "编辑优惠券")
    @PutMapping("/{id}")
    public Result<Coupon> update(@PathVariable Long id, @Valid @RequestBody CouponForm form) {
        return Result.ok(couponService.update(id, form));
    }

    @Operation(summary = "启用/停用优惠券")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        couponService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除优惠券（有领取记录时拒绝）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return Result.ok();
    }
}
