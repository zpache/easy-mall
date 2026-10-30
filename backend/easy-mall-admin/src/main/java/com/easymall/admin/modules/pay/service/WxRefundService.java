package com.easymall.admin.modules.pay.service;

import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.Order;
import com.easymall.admin.entity.OrderRefund;
import com.easymall.admin.modules.pay.support.WxPaySigner;
import com.easymall.admin.modules.setting.service.SysConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信支付 v3 退款（原路退回）。
 *
 * <p>只在「确认打款」时调用：未启用微信支付但开着模拟支付时返回 null，
 * 表示模拟退款（仅改状态，不动钱）；启用了微信支付则必须真实调用成功才允许置为已退款，
 * 失败直接抛异常让事务回滚，避免「状态显示已退款、实际没退钱」。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxRefundService {

    private static final String GROUP = "payment";
    private static final String API_BASE = "https://api.mch.weixin.qq.com";
    private static final String REFUND_PATH = "/v3/refund/domestic/refunds";
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final SysConfigService sysConfigService;
    private final ObjectMapper objectMapper;

    private final SecureRandom random = new SecureRandom();

    /**
     * 发起退款。
     *
     * @return 微信退款单号；null 表示模拟退款
     */
    public String refund(Order order, OrderRefund refund) {
        boolean wxpayEnabled = "true".equalsIgnoreCase(cfg("wxpayEnabled", "false"));
        boolean mockEnabled = "true".equalsIgnoreCase(cfg("mockPayEnabled", "true"));
        if (!wxpayEnabled) {
            if (mockEnabled) {
                log.info("模拟退款：orderNo={}, refundNo={}, 金额={}", order.getOrderNo(), refund.getRefundNo(), refund.getAmount());
                return null;
            }
            throw new BusinessException("微信支付未开启，无法原路退款；请开启支付渠道或走线下退款");
        }

        String appId = cfg("appId", "");
        String mchId = cfg("mchId", "");
        String serialNo = cfg("mchSerialNo", "");
        String apiV3Key = cfg("apiV3Key", "");
        String privateKey = cfg("privateKey", "");
        String notifyUrl = cfg("refundNotifyUrl", "");
        if (!StringUtils.hasText(mchId) || !StringUtils.hasText(serialNo)
                || !StringUtils.hasText(apiV3Key) || !StringUtils.hasText(privateKey)) {
            throw new BusinessException("微信支付参数未配置完整，无法退款");
        }

        long totalFen = toFen(order.getPayAmount());
        long refundFen = toFen(refund.getAmount());
        if (refundFen <= 0) {
            throw new BusinessException("退款金额必须大于 0");
        }
        if (refundFen > totalFen) {
            throw new BusinessException("退款金额不能超过订单实付金额");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("out_trade_no", order.getOrderNo());
        payload.put("out_refund_no", refund.getRefundNo());
        payload.put("reason", StringUtils.hasText(refund.getReason()) ? trimReason(refund.getReason()) : "售后退款");
        payload.put("amount", Map.of("refund", refundFen, "total", totalFen, "currency", "CNY"));
        if (StringUtils.hasText(notifyUrl)) {
            payload.put("notify_url", notifyUrl);
        }

        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BusinessException("退款参数序列化失败");
        }

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = randomString(32);
        String message = "POST\n" + REFUND_PATH + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        String signature = WxPaySigner.sign(privateKey, message);
        String authorization = "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + mchId + "\","
                + "nonce_str=\"" + nonce + "\","
                + "signature=\"" + signature + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + serialNo + "\"";

        try {
            String response = RestClient.create(API_BASE)
                    .post()
                    .uri(REFUND_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", authorization)
                    .header("Accept", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode node = objectMapper.readTree(response);
            String refundId = node.path("refund_id").asText();
            log.info("微信退款受理成功: orderNo={}, refundNo={}, 微信退款单号={}, 状态={}",
                    order.getOrderNo(), refund.getRefundNo(), refundId, node.path("status").asText());
            return StringUtils.hasText(refundId) ? refundId : null;
        } catch (RestClientResponseException e) {
            log.error("微信退款失败: orderNo={}, refundNo={}, status={}, body={}",
                    order.getOrderNo(), refund.getRefundNo(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("微信退款失败：" + extractError(e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("微信退款异常: orderNo={}, refundNo={}", order.getOrderNo(), refund.getRefundNo(), e);
            throw new BusinessException("微信退款请求异常，请稍后重试");
        }
    }

    /** 是否处于模拟退款模式（用于前端提示） */
    public boolean mockMode() {
        return !"true".equalsIgnoreCase(cfg("wxpayEnabled", "false"))
                && "true".equalsIgnoreCase(cfg("mockPayEnabled", "true"));
    }

    private long toFen(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /** 微信退款原因最多 80 字符 */
    private String trimReason(String reason) {
        return reason.length() > 80 ? reason.substring(0, 80) : reason;
    }

    private String extractError(String body) {
        if (!StringUtils.hasText(body)) {
            return "未知错误";
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            String message = node.path("message").asText();
            return StringUtils.hasText(message) ? message : body;
        } catch (Exception e) {
            return body;
        }
    }

    private String cfg(String key, String defaultValue) {
        return sysConfigService.get(GROUP, key, defaultValue);
    }

    private String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return builder.toString();
    }
}
