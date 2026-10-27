package com.easymall.admin.modules.system.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.SysUser;
import com.easymall.admin.modules.system.dto.SysUserForm;
import com.easymall.admin.modules.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台账号管理。
 */
@Tag(name = "系统-管理员")
@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @Operation(summary = "分页查询管理员")
    @GetMapping
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(sysUserService.pageQuery(pageNum, pageSize, keyword, status));
    }

    @Operation(summary = "新增管理员")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysUser> create(@Valid @RequestBody SysUserForm form) {
        return Result.ok(sysUserService.create(form));
    }

    @Operation(summary = "编辑管理员")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysUser> update(@PathVariable Long id, @Valid @RequestBody SysUserForm form) {
        return Result.ok(sysUserService.update(id, form));
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        sysUserService.resetPassword(id, password);
        return Result.ok();
    }

    @Operation(summary = "删除管理员")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.ok();
    }
}
