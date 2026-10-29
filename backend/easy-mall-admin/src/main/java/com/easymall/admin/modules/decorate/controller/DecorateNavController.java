package com.easymall.admin.modules.decorate.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.DecorateNav;
import com.easymall.admin.modules.decorate.dto.NavSaveForm;
import com.easymall.admin.modules.decorate.service.DecorateNavService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城装修：导航配置（底部 TabBar / 顶部导航 / 快捷入口）。
 */
@Tag(name = "装修-导航装修")
@RestController
@RequestMapping("/decorate/nav")
@RequiredArgsConstructor
public class DecorateNavController {

    private final DecorateNavService navService;

    @Operation(summary = "读取导航配置")
    @GetMapping
    public Result<DecorateNav> get() {
        return Result.ok(navService.get());
    }

    @Operation(summary = "保存导航配置（草稿，发布前不影响小程序）")
    @PutMapping
    public Result<DecorateNav> save(@Valid @RequestBody NavSaveForm form) {
        return Result.ok(navService.save(form));
    }

    @Operation(summary = "发布导航配置（草稿快照生效到小程序）")
    @PostMapping("/publish")
    public Result<DecorateNav> publish() {
        return Result.ok(navService.publish());
    }

    @Operation(summary = "恢复默认导航配置")
    @PostMapping("/reset")
    public Result<DecorateNav> reset() {
        return Result.ok(navService.reset());
    }
}
