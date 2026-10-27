package com.easymall.admin.modules.member.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.MemberTag;
import com.easymall.admin.modules.member.dto.TagForm;
import com.easymall.admin.modules.member.service.TagService;
import com.easymall.admin.modules.member.vo.TagVO;
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
 * 会员标签与分群。
 */
@Tag(name = "会员-标签分群")
@RestController
@RequestMapping("/member-tags")
@RequiredArgsConstructor
public class MemberTagController {

    private final TagService tagService;

    @Operation(summary = "分页查询标签（含会员数）")
    @GetMapping
    public Result<PageResult<TagVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) String keyword) {
        return Result.ok(tagService.pageQuery(pageNum, pageSize, keyword));
    }

    @Operation(summary = "全部标签（打标下拉用）")
    @GetMapping("/options")
    public Result<List<TagVO>> options() {
        return Result.ok(tagService.listAll());
    }

    @Operation(summary = "新增标签")
    @PostMapping
    public Result<MemberTag> create(@Valid @RequestBody TagForm form) {
        return Result.ok(tagService.create(form));
    }

    @Operation(summary = "编辑标签")
    @PutMapping("/{id}")
    public Result<MemberTag> update(@PathVariable Long id, @Valid @RequestBody TagForm form) {
        return Result.ok(tagService.update(id, form));
    }

    @Operation(summary = "删除标签（同时解除会员关联）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.ok();
    }
}
