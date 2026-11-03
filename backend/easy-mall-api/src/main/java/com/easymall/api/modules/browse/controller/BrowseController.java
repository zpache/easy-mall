package com.easymall.api.modules.browse.controller;

import com.easymall.api.common.api.PageResult;
import com.easymall.api.common.api.Result;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.modules.browse.service.BrowseService;
import com.easymall.api.modules.browse.vo.BrowseItemVO;
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
 * 浏览足迹。
 */
@Tag(name = "浏览足迹")
@RestController
@RequestMapping("/browse")
@RequiredArgsConstructor
public class BrowseController {

    private final BrowseService browseService;

    @Operation(summary = "记录一次商品浏览（详情页进入时调用，重复浏览只刷新时间）")
    @PostMapping("/{goodsId}")
    public Result<Void> record(@PathVariable Long goodsId) {
        browseService.record(UserContext.userId(), goodsId);
        return Result.ok();
    }

    @Operation(summary = "足迹分页列表（按最近浏览时间倒序）")
    @GetMapping
    public Result<PageResult<BrowseItemVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(browseService.pageQuery(UserContext.userId(), pageNum, pageSize));
    }
}
