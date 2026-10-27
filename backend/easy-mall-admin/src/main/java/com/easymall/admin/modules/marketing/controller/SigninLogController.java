package com.easymall.admin.modules.marketing.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.marketing.service.SigninService;
import com.easymall.admin.modules.marketing.vo.SigninLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 签到记录（只读）。
 */
@Tag(name = "营销-签到记录")
@RestController
@RequestMapping("/signin-logs")
@RequiredArgsConstructor
public class SigninLogController {

    private final SigninService signinService;

    @Operation(summary = "分页查询签到记录")
    @GetMapping
    public Result<PageResult<SigninLogVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) Long userId,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(signinService.pageLogs(pageNum, pageSize, userId, beginDate, endDate));
    }
}
