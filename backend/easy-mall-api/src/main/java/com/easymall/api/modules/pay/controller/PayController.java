package com.easymall.api.modules.pay.controller;

import com.easymall.api.common.api.Result;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.modules.pay.service.WxPayService;
import com.easymall.api.modules.pay.vo.PrepayVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 支付：小程序发起支付 + 微信支付结果回调。
 *
 * <p>回调接口在免登录白名单内（/pay/wxpay/notify），安全依赖微信支付签名校验，
 * 验签失败一律返回 500 让微信重试并留痕。</p>
 */
@Slf4j
@Tag(name = "支付")
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private final WxPayService wxPayService;

    @Operation(summary = "发起支付：返回小程序调起支付参数；mock=true 表示已走模拟支付并完成入账")
    @PostMapping("/wxpay/prepay")
    public Result<PrepayVO> prepay(@RequestParam Long orderId) {
        return Result.ok(wxPayService.prepay(UserContext.userId(), orderId));
    }

    @Operation(summary = "微信支付结果回调（微信服务器调用，免登录）")
    @PostMapping("/wxpay/notify")
    public ResponseEntity<Map<String, String>> notify(HttpServletRequest request, @RequestBody byte[] body) {
        try {
            wxPayService.handleNotify(request, body);
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (BusinessException e) {
            log.warn("微信支付回调处理失败: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("code", "FAIL", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("微信支付回调处理异常", e);
            return ResponseEntity.status(500).body(Map.of("code", "FAIL", "message", "处理失败"));
        }
    }
}
