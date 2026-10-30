package com.easymall.admin.modules.member.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.MemberLevel;
import com.easymall.admin.modules.member.dto.LevelDiscountSwitchForm;
import com.easymall.admin.modules.member.dto.LevelForm;
import com.easymall.admin.modules.member.service.LevelService;
import com.easymall.admin.modules.member.vo.LevelDiscountSwitchVO;
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
 * 会员等级与权益。
 */
@Tag(name = "会员-等级权益")
@RestController
@RequestMapping("/member-levels")
@RequiredArgsConstructor
public class MemberLevelController {

    private final LevelService levelService;

    @Operation(summary = "分页查询等级")
    @GetMapping
    public Result<PageResult<MemberLevel>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status) {
        return Result.ok(levelService.pageQuery(pageNum, pageSize, keyword, status));
    }

    @Operation(summary = "启用中的等级（下拉选项）")
    @GetMapping("/options")
    public Result<List<MemberLevel>> options() {
        return Result.ok(levelService.listEnabled());
    }

    @Operation(summary = "查询等级折扣总开关")
    @GetMapping("/discount-switch")
    public Result<LevelDiscountSwitchVO> discountSwitch() {
        return Result.ok(levelService.discountSwitch());
    }

    @Operation(summary = "开启/关闭等级折扣总开关")
    @PutMapping("/discount-switch")
    public Result<LevelDiscountSwitchVO> updateDiscountSwitch(@Valid @RequestBody LevelDiscountSwitchForm form) {
        return Result.ok(levelService.updateDiscountSwitch(form));
    }

    @Operation(summary = "新增等级")
    @PostMapping
    public Result<MemberLevel> create(@Valid @RequestBody LevelForm form) {
        return Result.ok(levelService.create(form));
    }

    @Operation(summary = "编辑等级")
    @PutMapping("/{id}")
    public Result<MemberLevel> update(@PathVariable Long id, @Valid @RequestBody LevelForm form) {
        return Result.ok(levelService.update(id, form));
    }

    @Operation(summary = "启用/停用等级")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        levelService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除等级（有会员时拒绝）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        levelService.delete(id);
        return Result.ok();
    }
}
