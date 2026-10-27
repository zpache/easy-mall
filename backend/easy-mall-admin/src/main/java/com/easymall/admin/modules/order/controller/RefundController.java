package com.easymall.admin.modules.order.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.order.dto.RefundAuditForm;
import com.easymall.admin.modules.order.service.RefundService;
import com.easymall.admin.modules.order.vo.RefundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 售后退款管理。
 */
@Tag(name = "订单-售后")
@RestController
@RequestMapping("/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @Operation(summary = "分页查询售后单")
    @GetMapping
    public Result<PageResult<RefundVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) String type) {
        return Result.ok(refundService.pageQuery(pageNum, pageSize, keyword, status, type));
    }

    @Operation(summary = "审核售后单（同意 / 拒绝）")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody RefundAuditForm form) {
        refundService.audit(id, form);
        return Result.ok();
    }

    @Operation(summary = "确认退款（打款完成）")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        refundService.confirm(id);
        return Result.ok();
    }
}
