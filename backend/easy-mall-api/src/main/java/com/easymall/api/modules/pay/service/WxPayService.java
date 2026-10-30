package com.easymall.api.modules.pay.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.entity.MallOrder;
import com.easymall.api.entity.SysConfig;
import com.easymall.api.mapper.MallOrderMapper;
import com.easymall.api.modules.order.service.OrderService;
import com.easymall.api.modules.pay.config.WxPayConfig;
import com.easymall.api.modules.pay.support.WxPaySigner;
import com.easymall.api.modules.pay.vo.PrepayVO;
import com.easymall.api.modules.setting.service.SysConfigService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信支付 v3：JSAPI 下单 + 支付回调处理。
 *
 * <p>商户参数全部来自「商城设置 - 支付配置」，改完最迟 30 秒生效（配置有缓存），
 * 不依赖 application.yml。</p>
 *
 * <p>回调采用「验签 → 解密 → 幂等更新订单」三步，签名验证不通过一律拒绝，
 * 防止伪造支付成功请求。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxPayService {

    private static final String GROUP = SysConfig.GROUP_PAYMENT;
    private static final String API_BASE = "https://api.mch.weixin.qq.com";
    private static final String JSAPI_PATH = "/v3/pay/transactions/jsapi";
    /** 回调时间戳允许的偏差（秒），防止重放 */
    private static final long NOTIFY_TOLERANCE_SECONDS = 300L;
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final SysConfigService sysConfigService;
    private final MallOrderMapper orderMapper;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    private final SecureRandom random = new SecureRandom();

    /**
     * 支付统一入口：启用微信支付则返回调起支付参数；未启用但开启模拟支付则返回 mock 标记，
     * 前端据此直接本地当作支付成功（由前端再调模拟支付接口落库）。
     */
    public PrepayVO prepay(Long userId, Long orderId) {
        WxPayConfig config = config();
        MallOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != MallOrder.STATUS_UNPAID) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单当前状态无需支付");
        }

        if (!config.enabled()) {
            if (config.mockEnabled()) {
                // 模拟支付：直接在此完成订单入账，前端拿到 mock=true 当支付成功处理
                orderService.pay(userId, orderId);
                return PrepayVO.mockResult();
            }
            throw new BusinessException(ResultCode.PAY_CHANNEL_CLOSED, "支付渠道未开启，请联系客服");
        }
        if (!config.complete()) {
            throw new BusinessException(ResultCode.PAY_NOT_CONFIGURED, "微信支付参数未配置完整，请联系管理员");
        }

        String prepayId = requestPrepayId(config, order);
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = randomString(32);
        // 小程序二次签名：appId \n timeStamp \n nonceStr \n package \n
        String packageValue = "prepay_id=" + prepayId;
        String message = config.appId() + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageValue + "\n";
        String paySign = WxPaySigner.sign(config.privateKey(), message);
        return new PrepayVO(false, config.appId(), timeStamp, nonceStr, packageValue, "RSA", paySign);
    }

    /**
     * 处理支付结果回调。
     *
     * @throws BusinessException 验签失败 / 报文异常，调用方需返回非 200 让微信重试
     */
    public void handleNotify(HttpServletRequest request, byte[] body) {
        WxPayConfig config = config();
        if (!config.notifyReady()) {
            log.error("微信支付回调到达，但未配置 APIv3 密钥或微信支付公钥，无法验签");
            throw new BusinessException(ResultCode.PAY_NOTIFY_INVALID, "支付回调未配置验签参数");
        }

        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String signature = request.getHeader("Wechatpay-Signature");
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature)) {
            throw new BusinessException(ResultCode.PAY_NOTIFY_INVALID, "回调缺少签名头");
        }
        // 时间戳防重放：超过 5 分钟的报文直接拒绝
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.PAY_NOTIFY_INVALID, "回调时间戳非法");
        }
        if (Math.abs(System.currentTimeMillis() / 1000 - ts) > NOTIFY_TOLERANCE_SECONDS) {
            throw new BusinessException(ResultCode.PAY_NOTIFY_INVALID, "回调时间戳超出允许范围");
        }

        String payload = new String(body, StandardCharsets.UTF_8);
        String message = timestamp + "\n" + nonce + "\n" + payload + "\n";
        if (!WxPaySigner.verify(config.publicKey(), message, signature)) {
            log.warn("微信支付回调验签失败: timestamp={}, nonce={}", timestamp, nonce);
            throw new BusinessException(ResultCode.PAY_NOTIFY_INVALID, "回调验签失败");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PAY_NOTIFY_INVALID, "回调报文解析失败");
        }
        String eventType = root.path("event_type").asString();
        if (!"TRANSACTION.SUCCESS".equals(eventType)) {
            // 非支付成功事件（如退款通知走另一个回调地址），直接应答成功避免微信重试
            log.info("忽略非支付成功事件: {}", eventType);
            return;
        }

        JsonNode resource = root.path("resource");
        String plain;
        try {
            plain = WxPaySigner.decrypt(config.apiV3Key(),
                    resource.path("nonce").asString(),
                    resource.path("associated_data").asString(),
                    resource.path("ciphertext").asString());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PAY_NOTIFY_INVALID, "回调报文解密失败");
        }

        JsonNode txn;
        try {
            txn = objectMapper.readTree(plain);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PAY_NOTIFY_INVALID, "回调明文字段解析失败");
        }
        String tradeState = txn.path("trade_state").asString();
        String outTradeNo = txn.path("out_trade_no").asString();
        String transactionId = txn.path("transaction_id").asString();
        if (!"SUCCESS".equals(tradeState)) {
            log.warn("微信支付回调状态非 SUCCESS: outTradeNo={}, tradeState={}", outTradeNo, tradeState);
            return;
        }
        // 金额校验：微信实付金额必须与订单应付一致，防止篡改
        long paidFen = txn.path("amount").path("total").asLong(0);
        boolean matched = orderService.markPaid(outTradeNo, transactionId, paidFen);
        if (!matched) {
            log.error("微信支付金额与订单应付不一致，需人工核对: outTradeNo={}, 微信实付={}分", outTradeNo, paidFen);
        }
    }

    /**
     * 关闭微信侧的预支付单：本地关单（用户取消 / 超时 / 后台关闭）后调用，
     * 防止用户停留在收银台时又付款成功（那笔钱会变成「待人工退款」）。
     *
     * <p>best-effort：订单在微信侧不存在（用户从未拉起支付）或已支付时微信都会报错，
     * 这里只记日志不抛出，本地关单结果不受影响。</p>
     */
    public void closeRemoteOrder(String outTradeNo) {
        WxPayConfig config = config();
        if (!config.enabled() || !config.complete()) {
            return;
        }
        String path = "/v3/pay/transactions/out-trade-no/" + outTradeNo + "/close";
        try {
            postV3(config, path, Map.of("mchid", config.mchId()));
            log.info("微信关单成功: outTradeNo={}", outTradeNo);
        } catch (Exception e) {
            log.warn("微信关单失败（不影响本地关单）: outTradeNo={}, err={}", outTradeNo, e.getMessage());
        }
    }

    /* ---------------- 微信支付 v3 下单 ---------------- */

    private String requestPrepayId(WxPayConfig config, MallOrder order) {
        long totalFen = order.getPayAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        if (totalFen <= 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单金额异常，无法发起支付");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appid", config.appId());
        payload.put("mchid", config.mchId());
        payload.put("description", "易商城订单 " + order.getOrderNo());
        payload.put("out_trade_no", order.getOrderNo());
        payload.put("notify_url", config.notifyUrl());
        payload.put("amount", Map.of("total", totalFen, "currency", "CNY"));

        try {
            String response = postV3(config, JSAPI_PATH, payload);
            JsonNode node = objectMapper.readTree(response);
            String prepayId = node.path("prepay_id").asString();
            if (!StringUtils.hasText(prepayId)) {
                log.error("微信支付下单未返回 prepay_id: {}", response);
                throw new BusinessException(ResultCode.PAY_FAILED, "微信支付下单失败，请稍后重试");
            }
            return prepayId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信支付 JSAPI 下单异常: orderNo={}", order.getOrderNo(), e);
            throw new BusinessException(ResultCode.PAY_FAILED, "拉起支付失败，请稍后重试");
        }
    }

    /** 带商户签名的 v3 POST 通用请求：微信 4xx/5xx 时抛 RestClientResponseException，由调用方决定如何处理 */
    private String postV3(WxPayConfig config, String path, Map<String, Object> payload) {
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PAY_FAILED, "支付请求参数序列化失败");
        }

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = randomString(32);
        String message = "POST\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        String signature = WxPaySigner.sign(config.privateKey(), message);
        String authorization = "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + config.mchId() + "\","
                + "nonce_str=\"" + nonce + "\","
                + "signature=\"" + signature + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + config.mchSerialNo() + "\"";

        return RestClient.create(API_BASE)
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authorization)
                .header("Accept", "application/json")
                .header("Wechatpay-Serial", config.publicKeyId() == null ? "" : config.publicKeyId())
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private WxPayConfig config() {
        return new WxPayConfig(
                sysConfigService.getBoolean(GROUP, "wxpayEnabled", false),
                sysConfigService.getBoolean(GROUP, "mockPayEnabled", true),
                sysConfigService.get(GROUP, "appId", ""),
                sysConfigService.get(GROUP, "mchId", ""),
                sysConfigService.get(GROUP, "mchSerialNo", ""),
                sysConfigService.get(GROUP, "apiV3Key", ""),
                sysConfigService.get(GROUP, "privateKey", ""),
                sysConfigService.get(GROUP, "notifyUrl", ""),
                sysConfigService.get(GROUP, "wxpayPublicKey", ""),
                sysConfigService.get(GROUP, "wxpayPublicKeyId", ""));
    }

    private String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return builder.toString();
    }

    /** 便于后台自检：配置是否可用于真实下单与回调 */
    public Map<String, Object> diagnostics() {
        WxPayConfig config = config();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enabled", config.enabled());
        map.put("mockEnabled", config.mockEnabled());
        map.put("complete", config.complete());
        map.put("notifyReady", config.notifyReady());
        map.put("serverTime", LocalDateTime.now().toString());
        return map;
    }

    /** 供订单服务兜底：订单是否已支付（回调用不到，保留给排查） */
    public boolean isPaid(String orderNo) {
        MallOrder order = orderMapper.selectOne(Wrappers.<MallOrder>lambdaQuery()
                .eq(MallOrder::getOrderNo, orderNo));
        return order != null && order.getPayTime() != null;
    }
}
