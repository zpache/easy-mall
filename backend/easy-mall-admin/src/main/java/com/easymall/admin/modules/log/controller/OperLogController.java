package com.easymall.admin.modules.log.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.SysOperLog;
import com.easymall.admin.modules.log.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询。
 */
@Tag(name = "系统-操作日志")
@RestController
@RequestMapping("/oper-logs")
@RequiredArgsConstructor
public class OperLogController {

    private final OperLogService operLogService;

    @Operation(summary = "操作日志分页")
    @GetMapping
    public Result<PageResult<SysOperLog>> page(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize,
                                               @RequestParam(required = false) String username,
                                               @RequestParam(required = false) String module,
                                               @RequestParam(required = false) Integer resultCode) {
        Page<SysOperLog> page = operLogService.pageQuery(pageNum, pageSize, username, module, resultCode);
        return Result.ok(new PageResult<>(page.getRecords(), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages()));
    }
}
