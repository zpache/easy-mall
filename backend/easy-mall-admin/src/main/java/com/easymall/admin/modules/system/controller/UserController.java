package com.easymall.admin.modules.system.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.SysUser;
import com.easymall.admin.modules.system.dto.UserForm;
import com.easymall.admin.modules.system.service.UserService;
import com.easymall.admin.modules.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统管理 - 用户管理。
 */
@Tag(name = "系统-用户管理")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/list")
    public Result<PageResult<UserVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long deptId,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(userService.pageQuery(pageNum, pageSize, keyword, deptId, status));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysUser> create(@Valid @RequestBody UserForm form) {
        return Result.ok(userService.create(form));
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysUser> update(@PathVariable Long id, @Valid @RequestBody UserForm form) {
        return Result.ok(userService.update(id, form));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "启用 / 停用用户")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status == null) {
            throw new BusinessException("状态不能为空");
        }
        userService.changeStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        userService.resetPassword(id, password);
        return Result.ok();
    }
}
