package com.easymall.admin.modules.decorate.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.decorate.service.DecorateNavService;
import com.easymall.admin.modules.decorate.service.DecoratePageService;
import com.easymall.admin.modules.decorate.vo.NavPreviewVO;
import com.easymall.admin.modules.decorate.vo.PagePreviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 真机扫码预览（免登录，仅暴露页面渲染结构）。
 */
@Tag(name = "装修-扫码预览")
@RestController
@RequestMapping("/decorate/preview")
@RequiredArgsConstructor
public class DecoratePreviewController {

    private final DecoratePageService pageService;
    private final DecorateNavService navService;

    @Operation(summary = "按预览码获取页面结构")
    @GetMapping("/{code}")
    public Result<PagePreviewVO> preview(@PathVariable String code) {
        return Result.ok(pageService.preview(code));
    }

    @Operation(summary = "获取导航配置（供预览页渲染 TabBar / 顶部导航 / 悬浮球）")
    @GetMapping("/nav")
    public Result<NavPreviewVO> nav() {
        return Result.ok(navService.previewConfig());
    }
}
