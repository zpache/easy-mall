package com.easymall.admin.modules.trade.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.trade.dto.FreightConfigForm;
import com.easymall.admin.modules.trade.dto.FreightRuleForm;
import com.easymall.admin.modules.trade.service.FreightService;
import com.easymall.admin.modules.trade.vo.FreightDetailVO;
import com.easymall.admin.modules.trade.vo.FreightRuleVO;
import com.easymall.admin.modules.trade.vo.RegionNodeVO;
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
 * 运费管理。
 */
@Tag(name = "商城设置-运费管理")
@RestController
@RequestMapping("/freight")
@RequiredArgsConstructor
public class FreightController {

    private final FreightService freightService;

    @Operation(summary = "运费管理详情（全局配置 + 区域规则）")
    @GetMapping
    public Result<FreightDetailVO> detail() {
        return Result.ok(freightService.detail());
    }

    @Operation(summary = "更新全局运费配置")
    @PutMapping("/config")
    public Result<Void> updateConfig(@Valid @RequestBody FreightConfigForm form) {
        freightService.updateConfig(form);
        return Result.ok();
    }

    @Operation(summary = "省 / 市两级区域树（规则选择器用）")
    @GetMapping("/regions")
    public Result<List<RegionNodeVO>> regions() {
        return Result.ok(freightService.regionTree());
    }

    @Operation(summary = "新增区域运费规则")
    @PostMapping("/rules")
    public Result<FreightRuleVO> createRule(@Valid @RequestBody FreightRuleForm form) {
        return Result.ok(freightService.createRule(form));
    }

    @Operation(summary = "编辑区域运费规则")
    @PutMapping("/rules/{id}")
    public Result<FreightRuleVO> updateRule(@PathVariable Long id, @Valid @RequestBody FreightRuleForm form) {
        return Result.ok(freightService.updateRule(id, form));
    }

    @Operation(summary = "删除区域运费规则")
    @DeleteMapping("/rules/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        freightService.deleteRule(id);
        return Result.ok();
    }
}
