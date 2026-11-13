package cn.easykits.admin.modules.member.controller;

import cn.easykits.admin.common.api.PageResult;
import cn.easykits.admin.common.api.Result;
import cn.easykits.admin.entity.MemberTag;
import cn.easykits.admin.modules.member.dto.TagForm;
import cn.easykits.admin.modules.member.service.TagService;
import cn.easykits.admin.modules.member.vo.TagVO;
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
    @PreAuthorize("hasAuthority('member:tag:list')")
    public Result<PageResult<TagVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) String keyword) {
        return Result.ok(tagService.pageQuery(pageNum, pageSize, keyword));
    }

    @Operation(summary = "全部标签（打标下拉用）")
    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('member:tag:list', 'member:member:list')")
    public Result<List<TagVO>> options() {
        return Result.ok(tagService.listAll());
    }

    @Operation(summary = "新增标签")
    @PostMapping
    @PreAuthorize("hasAuthority('member:tag:create')")
    public Result<MemberTag> create(@Valid @RequestBody TagForm form) {
        return Result.ok(tagService.create(form));
    }

    @Operation(summary = "编辑标签")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('member:tag:update')")
    public Result<MemberTag> update(@PathVariable Long id, @Valid @RequestBody TagForm form) {
        return Result.ok(tagService.update(id, form));
    }

    @Operation(summary = "删除标签（同时解除会员关联）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('member:tag:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.ok();
    }
}
