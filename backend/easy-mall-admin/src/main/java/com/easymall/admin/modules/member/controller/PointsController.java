package com.easymall.admin.modules.member.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.member.dto.PointsRuleForm;
import com.easymall.admin.modules.member.service.PointsService;
import com.easymall.admin.modules.member.vo.PointsLogVO;
import com.easymall.admin.modules.member.vo.PointsRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员积分：积分规则 + 流水查询（调整入口在 /members/{id}/points）。
 */
@Tag(name = "会员-积分")
@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    @Operation(summary = "积分规则（是否发放 + 每元积分）")
    @GetMapping("/rule")
    public Result<PointsRuleVO> rule() {
        return Result.ok(pointsService.rule());
    }

    @Operation(summary = "保存积分规则")
    @PutMapping("/rule")
    public Result<Void> updateRule(@Valid @RequestBody PointsRuleForm form) {
        pointsService.updateRule(form);
        return Result.ok();
    }

    @Operation(summary = "分页查询积分流水")
    @GetMapping
    public Result<PageResult<PointsLogVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) String type) {
        return Result.ok(pointsService.pageLogs(pageNum, pageSize, userId, type));
    }
}
