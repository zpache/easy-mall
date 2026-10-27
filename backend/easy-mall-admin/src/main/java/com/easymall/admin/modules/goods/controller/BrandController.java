package com.easymall.admin.modules.goods.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.Brand;
import com.easymall.admin.modules.goods.dto.BrandForm;
import com.easymall.admin.modules.goods.service.BrandService;
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

import java.util.List;

/**
 * 品牌管理。
 */
@Tag(name = "商品-品牌")
@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @Operation(summary = "分页查询品牌")
    @GetMapping
    public Result<PageResult<Brand>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer status) {
        return Result.ok(brandService.pageQuery(pageNum, pageSize, keyword, status));
    }

    @Operation(summary = "启用中的品牌（下拉选项）")
    @GetMapping("/options")
    public Result<List<Brand>> options() {
        return Result.ok(brandService.listEnabled());
    }

    @Operation(summary = "新增品牌")
    @PostMapping
    public Result<Brand> create(@Valid @RequestBody BrandForm form) {
        return Result.ok(brandService.create(form));
    }

    @Operation(summary = "编辑品牌")
    @PutMapping("/{id}")
    public Result<Brand> update(@PathVariable Long id, @Valid @RequestBody BrandForm form) {
        return Result.ok(brandService.update(id, form));
    }

    @Operation(summary = "启用/停用品牌")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        brandService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除品牌")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return Result.ok();
    }
}
