package com.easymall.api.modules.goods.controller;

import com.easymall.api.common.api.PageResult;
import com.easymall.api.common.api.Result;
import com.easymall.api.entity.Category;
import com.easymall.api.entity.Goods;
import com.easymall.api.modules.goods.service.GoodsService;
import com.easymall.api.modules.goods.vo.HomeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品浏览（免登录）。
 */
@Tag(name = "商品")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    @Operation(summary = "首页聚合数据")
    @GetMapping("/home")
    public Result<HomeVO> home() {
        return Result.ok(goodsService.home());
    }

    @Operation(summary = "分类列表")
    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.ok(goodsService.categories());
    }

    @Operation(summary = "商品列表")
    @GetMapping("/goods")
    public Result<PageResult<Goods>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String sortBy) {
        return Result.ok(goodsService.pageQuery(pageNum, pageSize, categoryId, keyword, sortBy));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/goods/{id}")
    public Result<Goods> detail(@PathVariable Long id) {
        return Result.ok(goodsService.detail(id));
    }
}
