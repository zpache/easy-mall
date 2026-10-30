package com.easymall.admin.modules.setting.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.setting.dto.PaymentConfigForm;
import com.easymall.admin.modules.setting.service.PaymentConfigService;
import com.easymall.admin.modules.setting.vo.ConfigCheckItemVO;
import com.easymall.admin.modules.setting.vo.PaymentConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城设置 - 支付配置。
 */
@Tag(name = "商城设置-支付配置")
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentConfigService paymentConfigService;

    @Operation(summary = "支付配置详情（敏感项脱敏）")
    @GetMapping("/config")
    public Result<PaymentConfigVO> detail() {
        return Result.ok(paymentConfigService.detail());
    }

    @Operation(summary = "保存支付配置")
    @PutMapping("/config")
    public Result<Void> update(@Valid @RequestBody PaymentConfigForm form) {
        paymentConfigService.update(form);
        return Result.ok();
    }

    @Operation(summary = "支付参数自检")
    @PostMapping("/check")
    public Result<List<ConfigCheckItemVO>> check() {
        return Result.ok(paymentConfigService.check());
    }
}
