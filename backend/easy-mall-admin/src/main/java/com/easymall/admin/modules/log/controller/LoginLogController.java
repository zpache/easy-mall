package com.easymall.admin.modules.log.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.SysLoginLog;
import com.easymall.admin.modules.log.service.LoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录日志查询。
 */
@Tag(name = "系统-登录日志")
@RestController
@RequestMapping("/login-logs")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @Operation(summary = "登录日志分页")
    @GetMapping
    @PreAuthorize("hasAuthority('system:loginlog:list')")
    public Result<PageResult<SysLoginLog>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String username,
                                                @RequestParam(required = false) Boolean success) {
        var page = loginLogService.pageQuery(pageNum, pageSize, username, success);
        return Result.ok(new PageResult<>(page.getRecords(), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages()));
    }
}
