package com.easymall.admin.modules.goods.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.goods.dto.SkuConfigForm;
import com.easymall.admin.modules.goods.service.SkuService;
import com.easymall.admin.modules.goods.vo.SkuConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品规格与 SKU。
 */
@Tag(name = "商品-规格")
@RestController
@RequestMapping("/goods/{goodsId}/sku")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    @Operation(summary = "查询商品的规格定义与 SKU 列表")
    @GetMapping
    public Result<SkuConfigVO> detail(@PathVariable Long goodsId) {
        return Result.ok(skuService.get(goodsId));
    }

    @Operation(summary = "保存规格与 SKU（specs 为空表示改回单规格）")
    @PutMapping
    public Result<SkuConfigVO> save(@PathVariable Long goodsId, @Valid @RequestBody SkuConfigForm form) {
        return Result.ok(skuService.save(goodsId, form));
    }
}
