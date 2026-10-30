package com.easymall.admin.modules.order.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.order.dto.DeliverForm;
import com.easymall.admin.modules.order.service.LogisticsService;
import com.easymall.admin.modules.order.service.OrderService;
import com.easymall.admin.modules.order.vo.LogisticsTraceVO;
import com.easymall.admin.modules.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 订单管理。待发货页面复用分页接口（status = 1）。
 */
@Tag(name = "订单-订单")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final LogisticsService logisticsService;

    @Operation(summary = "分页查询订单")
    @GetMapping
    public Result<PageResult<OrderVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) Integer refundStatus,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(orderService.pageQuery(pageNum, pageSize, keyword, status, refundStatus,
                startDate, endDate));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    @Operation(summary = "订单物流轨迹（快递100 实时查询）")
    @GetMapping("/{id}/logistics")
    public Result<LogisticsTraceVO> logistics(@PathVariable Long id) {
        return Result.ok(logisticsService.trace(orderService.requireEntity(id)));
    }

    @Operation(summary = "订单发货")
    @PutMapping("/{id}/deliver")
    public Result<Void> deliver(@PathVariable Long id, @Valid @RequestBody DeliverForm form) {
        orderService.deliver(id, form);
        return Result.ok();
    }

    @Operation(summary = "关闭订单")
    @PutMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id,
                              @RequestParam(required = false) String reason) {
        orderService.close(id, reason);
        return Result.ok();
    }
}
