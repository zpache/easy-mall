package com.easymall.admin.modules.goods.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.goods.dto.CategoryForm;
import com.easymall.admin.modules.goods.entity.Category;
import com.easymall.admin.modules.goods.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类管理。
 */
@Tag(name = "商品-分类")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类列表（平铺，按 sort 升序）")
    @GetMapping
    public Result<List<Category>> list() {
        return Result.ok(categoryService.listAll());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Category> create(@Valid @RequestBody CategoryForm form) {
        return Result.ok(categoryService.create(form));
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryForm form) {
        return Result.ok(categoryService.update(id, form));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
