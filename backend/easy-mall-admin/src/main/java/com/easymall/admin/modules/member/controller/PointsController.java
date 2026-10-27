package com.easymall.admin.modules.member.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.member.service.PointsService;
import com.easymall.admin.modules.member.vo.PointsLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员积分流水查询（调整入口在 /members/{id}/points）。
 */
@Tag(name = "会员-积分")
@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    @Operation(summary = "分页查询积分流水")
    @GetMapping
    public Result<PageResult<PointsLogVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) String type) {
        return Result.ok(pointsService.pageLogs(pageNum, pageSize, userId, type));
    }
}
