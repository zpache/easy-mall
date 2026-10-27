package com.easymall.admin.modules.goods.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.Goods;
import com.easymall.admin.modules.goods.dto.GoodsForm;
import com.easymall.admin.modules.goods.service.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品管理。
 */
@Tag(name = "商品-商品")
@RestController
@RequestMapping("/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    @Operation(summary = "分页查询商品")
    @GetMapping
    public Result<PageResult<Goods>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) Integer status) {
        return Result.ok(goodsService.pageQuery(pageNum, pageSize, keyword, categoryId, status));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<Goods> detail(@PathVariable Long id) {
        return Result.ok(goodsService.detail(id));
    }

    @Operation(summary = "新增商品")
    @PostMapping
    public Result<Goods> create(@Valid @RequestBody GoodsForm form) {
        return Result.ok(goodsService.create(form));
    }

    @Operation(summary = "编辑商品")
    @PutMapping("/{id}")
    public Result<Goods> update(@PathVariable Long id, @Valid @RequestBody GoodsForm form) {
        return Result.ok(goodsService.update(id, form));
    }

    @Operation(summary = "上架/下架")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        goodsService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goodsService.delete(id);
        return Result.ok();
    }
}
