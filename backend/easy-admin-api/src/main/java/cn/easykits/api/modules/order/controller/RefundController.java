package cn.easykits.api.modules.order.controller;

import cn.easykits.api.common.api.PageResult;
import cn.easykits.api.common.api.Result;
import cn.easykits.api.common.context.UserContext;
import cn.easykits.api.modules.order.dto.RefundApplyForm;
import cn.easykits.api.modules.order.service.RefundService;
import cn.easykits.api.modules.order.vo.RefundApplyVO;
import cn.easykits.api.modules.order.vo.RefundRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 售后：小程序发起退款申请，列表/进度查询与撤销；审核与打款在管理端完成。
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

    @Operation(summary = "我的售后分页（status 为空查全部）")
    @GetMapping
    public Result<PageResult<RefundRecordVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize,
                                                   @RequestParam(required = false) Integer status) {
        return Result.ok(refundService.pageMy(UserContext.userId(), pageNum, pageSize, status));
    }

    @Operation(summary = "售后详情（含进度时间线）")
    @GetMapping("/{id}")
    public Result<RefundRecordVO> detail(@PathVariable Long id) {
        return Result.ok(refundService.detail(UserContext.userId(), id));
    }

    @Operation(summary = "撤销售后申请（仅待审核可撤销）")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        refundService.cancel(UserContext.userId(), id);
        return Result.ok();
    }
}
