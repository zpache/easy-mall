package com.easymall.admin.modules.decorate.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.DecorateTheme;
import com.easymall.admin.modules.decorate.dto.ThemeSaveForm;
import com.easymall.admin.modules.decorate.service.DecorateThemeService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城主题换肤。
 */
@Tag(name = "装修-主题换肤")
@RestController
@RequestMapping("/decorate/themes")
@RequiredArgsConstructor
public class DecorateThemeController {

    private final DecorateThemeService themeService;

    @Operation(summary = "全部主题（含草稿态）")
    @GetMapping
    public Result<List<DecorateTheme>> list() {
        return Result.ok(themeService.listAll());
    }

    @Operation(summary = "新增主题")
    @PostMapping
    public Result<DecorateTheme> create(@Valid @RequestBody ThemeSaveForm form) {
        return Result.ok(themeService.create(form));
    }

    @Operation(summary = "编辑主题（草稿，不影响线上）")
    @PutMapping("/{id}")
    public Result<DecorateTheme> update(@PathVariable Long id, @Valid @RequestBody ThemeSaveForm form) {
        return Result.ok(themeService.update(id, form));
    }

    @Operation(summary = "启用主题（发布快照并切换线上，全局唯一）")
    @PutMapping("/{id}/activate")
    public Result<DecorateTheme> activate(@PathVariable Long id) {
        return Result.ok(themeService.activate(id));
    }

    @Operation(summary = "删除主题（线上主题不可删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        themeService.delete(id);
        return Result.ok();
    }
}
