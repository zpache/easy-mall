package cn.easykits.admin.modules.goods.controller;

import cn.easykits.admin.common.api.PageResult;
import cn.easykits.admin.common.api.Result;
import cn.easykits.admin.entity.Brand;
import cn.easykits.admin.modules.goods.dto.BrandForm;
import cn.easykits.admin.modules.goods.service.BrandService;
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
    @PreAuthorize("hasAuthority('goods:brand:list')")
    public Result<PageResult<Brand>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer status) {
        return Result.ok(brandService.pageQuery(pageNum, pageSize, keyword, status));
    }

    @Operation(summary = "启用中的品牌（下拉选项）")
    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('goods:brand:list', 'goods:goods:list', "
            + "'goods:goods:create', 'goods:goods:update')")
    public Result<List<Brand>> options() {
        return Result.ok(brandService.listEnabled());
    }

    @Operation(summary = "新增品牌")
    @PostMapping
    @PreAuthorize("hasAuthority('goods:brand:create')")
    public Result<Brand> create(@Valid @RequestBody BrandForm form) {
        return Result.ok(brandService.create(form));
    }

    @Operation(summary = "编辑品牌")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('goods:brand:update')")
    public Result<Brand> update(@PathVariable Long id, @Valid @RequestBody BrandForm form) {
        return Result.ok(brandService.update(id, form));
    }

    @Operation(summary = "启用/停用品牌")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('goods:brand:status')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        brandService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除品牌")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('goods:brand:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return Result.ok();
    }
}
