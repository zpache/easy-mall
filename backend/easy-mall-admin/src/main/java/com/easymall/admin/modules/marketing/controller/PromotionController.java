package com.easymall.admin.modules.marketing.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.Promotion;
import com.easymall.admin.modules.marketing.dto.PromotionForm;
import com.easymall.admin.modules.marketing.service.PromotionService;
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
 * 拼团秒杀活动。
 */
@Tag(name = "营销-拼团秒杀")
@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @Operation(summary = "分页查询活动")
    @GetMapping
    public Result<PageResult<Promotion>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer type,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(promotionService.pageQuery(pageNum, pageSize, keyword, type, status));
    }

    @Operation(summary = "新增活动")
    @PostMapping
    public Result<Promotion> create(@Valid @RequestBody PromotionForm form) {
        return Result.ok(promotionService.create(form));
    }

    @Operation(summary = "编辑活动")
    @PutMapping("/{id}")
    public Result<Promotion> update(@PathVariable Long id, @Valid @RequestBody PromotionForm form) {
        return Result.ok(promotionService.update(id, form));
    }

    @Operation(summary = "启用/停用活动")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        promotionService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除活动（已售出时拒绝）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return Result.ok();
    }
}
