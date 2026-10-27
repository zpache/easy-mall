package com.easymall.admin.modules.system.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.SysDept;
import com.easymall.admin.modules.system.dto.DeptForm;
import com.easymall.admin.modules.system.service.DeptService;
import com.easymall.admin.modules.system.vo.DeptVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统管理 - 部门管理。
 */
@Tag(name = "系统-部门管理")
@RestController
@RequestMapping("/system/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "部门树")
    @GetMapping("/list")
    public Result<List<DeptVO>> list() {
        return Result.ok(deptService.listTree());
    }

    @Operation(summary = "新增部门")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysDept> create(@Valid @RequestBody DeptForm form) {
        return Result.ok(deptService.create(form));
    }

    @Operation(summary = "编辑部门")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysDept> update(@PathVariable Long id, @Valid @RequestBody DeptForm form) {
        return Result.ok(deptService.update(id, form));
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok();
    }
}
