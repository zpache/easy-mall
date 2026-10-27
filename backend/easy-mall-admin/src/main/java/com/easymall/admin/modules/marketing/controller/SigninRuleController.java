package com.easymall.admin.modules.marketing.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.SigninRule;
import com.easymall.admin.modules.marketing.dto.SigninRuleForm;
import com.easymall.admin.modules.marketing.service.SigninService;
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
 * 签到规则。
 */
@Tag(name = "营销-签到规则")
@RestController
@RequestMapping("/signin-rules")
@RequiredArgsConstructor
public class SigninRuleController {

    private final SigninService signinService;

    @Operation(summary = "分页查询签到规则")
    @GetMapping
    public Result<PageResult<SigninRule>> page(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(signinService.pageRules(pageNum, pageSize, keyword, status));
    }

    @Operation(summary = "新增签到规则")
    @PostMapping
    public Result<SigninRule> create(@Valid @RequestBody SigninRuleForm form) {
        return Result.ok(signinService.createRule(form));
    }

    @Operation(summary = "编辑签到规则")
    @PutMapping("/{id}")
    public Result<SigninRule> update(@PathVariable Long id, @Valid @RequestBody SigninRuleForm form) {
        return Result.ok(signinService.updateRule(id, form));
    }

    @Operation(summary = "启用/停用签到规则")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        signinService.updateRuleStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除签到规则")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        signinService.deleteRule(id);
        return Result.ok();
    }
}
