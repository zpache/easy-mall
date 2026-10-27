package com.easymall.admin.modules.marketing.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.Reduction;
import com.easymall.admin.modules.marketing.dto.ReductionForm;
import com.easymall.admin.modules.marketing.service.ReductionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * 满减活动。
 */
@Tag(name = "营销-满减活动")
@RestController
@RequestMapping("/reductions")
@RequiredArgsConstructor
public class ReductionController {

    private final ReductionService reductionService;

    @Operation(summary = "分页查询满减活动")
    @GetMapping
    public Result<PageResult<Reduction>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer mode,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(reductionService.pageQuery(pageNum, pageSize, keyword, mode, status));
    }

    @Operation(summary = "新增满减活动")
    @PostMapping
    public Result<Reduction> create(@Valid @RequestBody ReductionForm form) {
        return Result.ok(reductionService.create(form));
    }

    @Operation(summary = "编辑满减活动")
    @PutMapping("/{id}")
    public Result<Reduction> update(@PathVariable Long id, @Valid @RequestBody ReductionForm form) {
        return Result.ok(reductionService.update(id, form));
    }

    @Operation(summary = "启用/停用满减活动")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        reductionService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除满减活动")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reductionService.delete(id);
        return Result.ok();
    }
}
