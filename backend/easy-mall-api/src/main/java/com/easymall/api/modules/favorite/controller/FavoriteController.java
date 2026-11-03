package com.easymall.api.modules.favorite.controller;

import com.easymall.api.common.api.PageResult;
import com.easymall.api.common.api.Result;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.modules.favorite.service.FavoriteService;
import com.easymall.api.modules.favorite.vo.FavoriteItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品收藏。
 */
@Tag(name = "商品收藏")
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "切换收藏（收藏/取消收藏），返回切换后是否已收藏")
    @PostMapping("/{goodsId}/toggle")
    public Result<Boolean> toggle(@PathVariable Long goodsId) {
        return Result.ok(favoriteService.toggle(UserContext.userId(), goodsId));
    }

    @Operation(summary = "商品是否已收藏")
    @GetMapping("/{goodsId}/status")
    public Result<Boolean> status(@PathVariable Long goodsId) {
        return Result.ok(favoriteService.isFavorited(UserContext.userId(), goodsId));
    }

    @Operation(summary = "收藏分页列表")
    @GetMapping
    public Result<PageResult<FavoriteItemVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(favoriteService.pageQuery(UserContext.userId(), pageNum, pageSize));
    }
}
