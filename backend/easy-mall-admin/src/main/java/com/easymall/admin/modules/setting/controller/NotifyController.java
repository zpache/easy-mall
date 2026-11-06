package com.easymall.admin.modules.setting.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.setting.dto.NotifyConfigForm;
import com.easymall.admin.modules.setting.service.NotifyConfigService;
import com.easymall.admin.modules.setting.vo.NotifyConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城设置 - 通知配置（微信订阅消息）。
 */
@Tag(name = "商城设置-通知配置")
@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyConfigService notifyConfigService;

    @Operation(summary = "通知配置详情（密钥脱敏）")
    @GetMapping("/config")
    @PreAuthorize("hasAuthority('settings:notify:list')")
    public Result<NotifyConfigVO> detail() {
        return Result.ok(notifyConfigService.detail());
    }

    @Operation(summary = "保存通知配置")
    @PutMapping("/config")
    @PreAuthorize("hasAuthority('settings:notify:update')")
    public Result<Void> update(@RequestBody NotifyConfigForm form) {
        notifyConfigService.update(form);
        return Result.ok();
    }
}
