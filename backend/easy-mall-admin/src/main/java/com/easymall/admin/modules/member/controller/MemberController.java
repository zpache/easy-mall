package com.easymall.admin.modules.member.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.member.dto.MemberTagForm;
import com.easymall.admin.modules.member.dto.PointsAdjustForm;
import com.easymall.admin.modules.member.service.MemberService;
import com.easymall.admin.modules.member.vo.MemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员管理：列表 / 详情 / 打标签 / 积分调整。
 */
@Tag(name = "会员-会员")
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "分页查询会员（关键词 / 等级 / 来源渠道 / 标签）")
    @GetMapping
    public Result<PageResult<MemberVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long levelId,
                                             @RequestParam(required = false) Integer source,
                                             @RequestParam(required = false) Long tagId) {
        return Result.ok(memberService.pageQuery(pageNum, pageSize, keyword, levelId, source, tagId));
    }

    @Operation(summary = "会员详情")
    @GetMapping("/{id}")
    public Result<MemberVO> detail(@PathVariable Long id) {
        return Result.ok(memberService.detail(id));
    }

    @Operation(summary = "会员打标签（全量覆盖）")
    @PutMapping("/{id}/tags")
    public Result<Void> updateTags(@PathVariable Long id, @Valid @RequestBody MemberTagForm form) {
        memberService.updateTags(id, form);
        return Result.ok();
    }

    @Operation(summary = "会员积分人工调整")
    @PutMapping("/{id}/points")
    public Result<Void> adjustPoints(@PathVariable Long id, @Valid @RequestBody PointsAdjustForm form) {
        memberService.adjustPoints(id, form.change(), form.remark());
        return Result.ok();
    }
}
