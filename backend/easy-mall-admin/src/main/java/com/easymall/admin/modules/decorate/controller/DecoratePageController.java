package com.easymall.admin.modules.decorate.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.DecoratePage;
import com.easymall.admin.modules.decorate.dto.PageForm;
import com.easymall.admin.modules.decorate.dto.PageSaveForm;
import com.easymall.admin.modules.decorate.service.DecoratePageService;
import com.easymall.admin.modules.decorate.vo.PageListVO;
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

/**
 * 商城装修：页面管理与草稿保存。
 */
@Tag(name = "装修-页面管理")
@RestController
@RequestMapping("/decorate/pages")
@RequiredArgsConstructor
public class DecoratePageController {

    private final DecoratePageService pageService;

    @Operation(summary = "分页查询装修页面")
    @GetMapping
    public Result<PageResult<PageListVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer type,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(pageService.pageQuery(pageNum, pageSize, keyword, type, status));
    }

    @Operation(summary = "页面详情（含草稿结构）")
    @GetMapping("/{id}")
    public Result<DecoratePage> detail(@PathVariable Long id) {
        return Result.ok(pageService.detail(id));
    }

    @Operation(summary = "新建页面")
    @PostMapping
    public Result<DecoratePage> create(@Valid @RequestBody PageForm form) {
        return Result.ok(pageService.create(form));
    }

    @Operation(summary = "保存草稿")
    @PutMapping("/{id}")
    public Result<DecoratePage> save(@PathVariable Long id, @Valid @RequestBody PageSaveForm form) {
        return Result.ok(pageService.saveSchema(id, form));
    }

    @Operation(summary = "发布页面")
    @PostMapping("/{id}/publish")
    public Result<DecoratePage> publish(@PathVariable Long id) {
        return Result.ok(pageService.publish(id));
    }

    @Operation(summary = "复制页面")
    @PostMapping("/{id}/copy")
    public Result<DecoratePage> copy(@PathVariable Long id) {
        return Result.ok(pageService.copy(id));
    }

    @Operation(summary = "设为商城首页")
    @PutMapping("/{id}/home")
    public Result<DecoratePage> setHome(@PathVariable Long id) {
        return Result.ok(pageService.setHome(id));
    }

    @Operation(summary = "重置预览码")
    @PutMapping("/{id}/preview-code")
    public Result<DecoratePage> resetPreviewCode(@PathVariable Long id) {
        return Result.ok(pageService.resetPreviewCode(id));
    }

    @Operation(summary = "删除页面（首页不可删）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        pageService.delete(id);
        return Result.ok();
    }
}
