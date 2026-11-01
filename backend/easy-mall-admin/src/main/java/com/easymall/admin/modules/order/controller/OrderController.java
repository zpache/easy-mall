package com.easymall.admin.modules.order.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.order.dto.DeliverForm;
import com.easymall.admin.modules.order.service.LogisticsService;
import com.easymall.admin.modules.order.service.OrderExcelService;
import com.easymall.admin.modules.order.service.OrderService;
import com.easymall.admin.modules.order.vo.BatchDeliverVO;
import com.easymall.admin.modules.order.vo.LogisticsTraceVO;
import com.easymall.admin.modules.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private final OrderExcelService orderExcelService;

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

    @Operation(summary = "订单导出（xlsx，沿用列表筛选条件，上限 5000 行）")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(required = false) Integer refundStatus,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        byte[] bytes = orderExcelService.exportOrders(keyword, status, refundStatus, startDate, endDate);
        String filename = URLEncoder.encode("订单导出_%s.xlsx".formatted(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @Operation(summary = "批量发货模板下载（xlsx）")
    @GetMapping("/deliver-template")
    public ResponseEntity<byte[]> deliverTemplate() {
        byte[] bytes = orderExcelService.deliverTemplate();
        String filename = URLEncoder.encode("批量发货模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @Operation(summary = "批量发货（上传按模板填写的 xlsx）")
    @PostMapping("/batch-deliver")
    public Result<BatchDeliverVO> batchDeliver(@RequestParam("file") MultipartFile file) {
        return Result.ok(orderExcelService.parseAndBatchDeliver(file));
    }
}
