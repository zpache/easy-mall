package com.easymall.api.modules.order.controller;

import com.easymall.api.common.api.Result;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.modules.order.dto.RefundApplyForm;
import com.easymall.api.modules.order.service.RefundService;
import com.easymall.api.modules.order.vo.RefundApplyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 售后：小程序发起退款申请，后续审核与打款在管理端完成。
 */
@Tag(name = "售后")
@RestController
@RequestMapping("/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @Operation(summary = "申请退款（金额由服务端按实付−已退计算）")
    @PostMapping("/apply")
    public Result<RefundApplyVO> apply(@Valid @RequestBody RefundApplyForm form) {
        return Result.ok(refundService.apply(UserContext.userId(), form));
    }
}
