package com.easymall.admin.modules.goods.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.Category;
import com.easymall.admin.modules.goods.dto.CategoryForm;
import com.easymall.admin.modules.goods.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('goods:category:list')")
    public Result<List<Category>> list() {
        return Result.ok(categoryService.listAll());
    }

    @Operation(summary = "分类树（分类管理页与下拉树使用）")
    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('goods:category:list', 'goods:category:create', "
            + "'goods:category:update', 'goods:goods:list', 'goods:goods:create', 'goods:goods:update')")
    public Result<List<Category>> tree() {
        return Result.ok(categoryService.listTree());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    @PreAuthorize("hasAuthority('goods:category:create')")
    public Result<Category> create(@Valid @RequestBody CategoryForm form) {
        return Result.ok(categoryService.create(form));
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('goods:category:update')")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryForm form) {
        return Result.ok(categoryService.update(id, form));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('goods:category:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
