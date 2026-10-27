package com.easymall.admin.modules.system.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.SysRole;
import com.easymall.admin.modules.system.dto.RoleForm;
import com.easymall.admin.modules.system.service.RoleService;
import com.easymall.admin.modules.system.vo.RoleVO;
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

import java.util.List;

/**
 * 系统管理 - 角色管理。
 */
@Tag(name = "系统-角色管理")
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "分页查询角色")
    @GetMapping("/list")
    public Result<PageResult<RoleVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String name,
                                           @RequestParam(required = false) String code,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(roleService.pageQuery(pageNum, pageSize, name, code, status));
    }

    @Operation(summary = "启用中的角色选项")
    @GetMapping("/options")
    public Result<List<RoleVO>> options() {
        return Result.ok(roleService.listEnabled());
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysRole> create(@Valid @RequestBody RoleForm form) {
        return Result.ok(roleService.create(form));
    }

    @Operation(summary = "编辑角色")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysRole> update(@PathVariable Long id, @Valid @RequestBody RoleForm form) {
        return Result.ok(roleService.update(id, form));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "启用 / 停用角色")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status == null) {
            throw new BusinessException("状态不能为空");
        }
        roleService.changeStatus(id, status);
        return Result.ok();
    }
}
