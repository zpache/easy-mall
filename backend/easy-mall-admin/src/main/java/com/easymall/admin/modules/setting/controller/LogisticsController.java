package com.easymall.admin.modules.setting.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.setting.dto.LogisticsConfigForm;
import com.easymall.admin.modules.setting.service.LogisticsConfigService;
import com.easymall.admin.modules.setting.vo.LogisticsConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城设置 - 物流配置（快递100）。
 */
@Tag(name = "商城设置-物流配置")
@RestController
@RequestMapping("/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsConfigService logisticsConfigService;

    @Operation(summary = "物流配置详情（密钥脱敏）")
    @GetMapping("/config")
    public Result<LogisticsConfigVO> detail() {
        return Result.ok(logisticsConfigService.detail());
    }

    @Operation(summary = "保存物流配置")
    @PutMapping("/config")
    public Result<Void> update(@RequestBody LogisticsConfigForm form) {
        logisticsConfigService.update(form);
        return Result.ok();
    }
}
