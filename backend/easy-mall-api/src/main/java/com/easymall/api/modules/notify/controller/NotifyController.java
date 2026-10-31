package com.easymall.api.modules.notify.controller;

import com.easymall.api.common.api.Result;
import com.easymall.api.entity.SysConfig;
import com.easymall.api.modules.setting.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息通知：小程序端订阅授权所需的模板 ID。
 */
@Tag(name = "消息通知")
@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "已启用的订阅消息模板 ID 列表（未启用返回空）")
    @GetMapping("/subscribe-templates")
    public Result<List<String>> subscribeTemplates() {
        List<String> templates = new ArrayList<>();
        if (sysConfigService.getBoolean(SysConfig.GROUP_NOTIFY, "subscribeEnabled", false)) {
            String deliver = sysConfigService.get(SysConfig.GROUP_NOTIFY, "tmplDeliver", "").trim();
            String refund = sysConfigService.get(SysConfig.GROUP_NOTIFY, "tmplRefund", "").trim();
            if (!deliver.isBlank()) {
                templates.add(deliver);
            }
            if (!refund.isBlank()) {
                templates.add(refund);
            }
        }
        return Result.ok(templates);
    }
}
