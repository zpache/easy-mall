package com.easymall.api.modules.order.controller;

import com.easymall.api.common.api.PageResult;
import com.easymall.api.common.api.Result;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.modules.order.dto.BuyNowRequest;
import com.easymall.api.modules.order.dto.OrderCreateRequest;
import com.easymall.api.modules.order.service.OrderService;
import com.easymall.api.modules.order.vo.OrderVO;
import com.easymall.api.modules.order.vo.ReductionPreviewVO;
import com.easymall.api.modules.order.vo.UsableCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单。
 */
@Tag(name = "订单")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "提交订单（购物车已勾选商品）")
    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateRequest request) {
        return Result.ok(orderService.create(UserContext.userId(), request));
    }

    @Operation(summary = "立即购买下单（不走购物车，不影响购物车数据）")
    @PostMapping("/buy-now")
    public Result<OrderVO> buyNow(@Valid @RequestBody BuyNowRequest request) {
        return Result.ok(orderService.createBuyNow(UserContext.userId(), request));
    }

    @Operation(summary = "结算页可用优惠券（默认按购物车已勾选商品；传 goodsId+quantity 时按立即购买商品计算）")
    @GetMapping("/coupons")
    public Result<List<UsableCouponVO>> usableCoupons(@RequestParam(required = false) Long goodsId,
                                                      @RequestParam(required = false) Integer quantity) {
        return Result.ok(orderService.usableCoupons(UserContext.userId(), goodsId, quantity));
    }

    @Operation(summary = "结算页满减预估（默认按购物车已勾选商品；传 goodsId+quantity 时按立即购买商品计算）")
    @GetMapping("/promotions")
    public Result<List<ReductionPreviewVO>> promotions(@RequestParam(required = false) Long goodsId,
                                                       @RequestParam(required = false) Integer quantity) {
        return Result.ok(orderService.promotionPreview(UserContext.userId(), goodsId, quantity));
    }

    @Operation(summary = "订单列表")
    @GetMapping
    public Result<PageResult<OrderVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(orderService.pageQuery(UserContext.userId(), status, pageNum, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(UserContext.userId(), id));
    }

    @Operation(summary = "模拟支付")
    @PutMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(UserContext.userId(), id);
        return Result.ok();
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(UserContext.userId(), id);
        return Result.ok();
    }

    @Operation(summary = "确认收货")
    @PutMapping("/{id}/receive")
    public Result<Void> confirmReceive(@PathVariable Long id) {
        orderService.confirmReceive(UserContext.userId(), id);
        return Result.ok();
    }
}
