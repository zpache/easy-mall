package com.easymall.admin.modules.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.admin.entity.AppUser;
import com.easymall.admin.entity.Order;
import com.easymall.admin.entity.OrderRefund;
import com.easymall.admin.entity.SysConfig;
import com.easymall.admin.mapper.AppUserMapper;
import com.easymall.admin.modules.setting.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信订阅消息推送：发货提醒、退款结果通知。
 *
 * <p>链路：小程序端支付成功时调 wx.requestSubscribeMessage 拿到一次性订阅授权，
 * 管理后台发货 / 退款落定后由本服务推送。模板字段不写死：按商家申请的模板字段类型
 * （订单号 / 商品 / 时间 / 状态 / 金额 / 快递）动态匹配，字段顺序以模板定义为准。</p>
 *
 * <p>推送永远不影响主流程：未启用 / 未配置模板 / 用户未订阅（errcode 43101）等
 * 一切异常只记日志。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxSubscribeService {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/stable_token";
    private static final String SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";
    private static final String TEMPLATE_URL = "https://api.weixin.qq.com/wxaapi/newtmpl/gettemplate";

    /** thing 字段微信限 20 字符 */
    private static final int THING_MAX = 20;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SysConfigService sysConfigService;
    private final AppUserMapper appUserMapper;
    private final ObjectMapper objectMapper;

    /** access_token 缓存（仅 admin 进程写，进程内缓存即可） */
    private volatile String accessToken;
    private volatile long tokenExpireAt;

    /**
     * 发货提醒。
     */
    public void notifyDeliver(Order order, String goodsSummary) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("character_string", order.getOrderNo());
        values.put("thing", truncate(goodsSummary));
        values.put("time", format(order.getDeliveryTime()));
        values.put("phrase", "已发货");
        send(order.getUserId(), "tmplDeliver", "/pages/order/detail?id=" + order.getId(), values);
    }

    /**
     * 退款结果通知（拒绝 / 已退款）。
     */
    public void notifyRefundResult(Order order, OrderRefund refund, String statusText) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("character_string", refund.getRefundNo());
        values.put("thing", truncate(refund.getReason()));
        values.put("amount", formatMoney(refund.getAmount()));
        values.put("time", format(LocalDateTime.now()));
        values.put("phrase", truncate(statusText));
        send(order.getUserId(), "tmplRefund", "/pages/order/detail?id=" + order.getId(), values);
    }

    /* ---------------- private ---------------- */

    private void send(Long userId, String tmplKey, String page, Map<String, String> values) {
        try {
            Map<String, String> config = sysConfigService.group(SysConfig.GROUP_NOTIFY);
            if (!"true".equalsIgnoreCase(config.get("subscribeEnabled"))) {
                return;
            }
            String tmplId = config.getOrDefault(tmplKey, "").trim();
            if (tmplId.isBlank()) {
                return;
            }
            String appId = config.getOrDefault("wxAppId", "").trim();
            String secret = config.getOrDefault("wxAppSecret", "").trim();
            if (appId.isBlank() || secret.isBlank()) {
                return;
            }
            AppUser user = appUserMapper.selectOne(Wrappers.<AppUser>lambdaQuery()
                    .eq(AppUser::getId, userId)
                    .last("LIMIT 1"));
            if (user == null || user.getOpenid() == null || user.getOpenid().isBlank()) {
                log.warn("订阅消息跳过：用户不存在或无 openid, userId={}", userId);
                return;
            }
            Map<String, Object> data = buildData(tmplId, values, appId, secret);
            if (data == null) {
                return;
            }
            doSend(user.getOpenid(), tmplId, page, data);
        } catch (Exception e) {
            // 推送不影响主流程
            log.warn("订阅消息发送失败, userId={}, tmplKey={}", userId, tmplKey, e);
        }
    }

    /**
     * 按模板字段类型动态组装 data：取回小程序后台的模板定义，
     * 按字段类型顺序（订单号/商品/金额/时间/状态/快递）填充。
     */
    private Map<String, Object> buildData(String tmplId, Map<String, String> values, String appId, String secret) {
        try {
            String body = RestClient.create().get()
                    .uri(TEMPLATE_URL + "?access_token={token}", getAccessToken(appId, secret))
                    .retrieve()
                    .body(String.class);
            var root = objectMapper.readTree(body);
            if (root.path("errcode").asInt(0) != 0) {
                log.warn("获取订阅消息模板失败: {}", body);
                return null;
            }
            for (var tmpl : root.path("data")) {
                if (!tmplId.equals(tmpl.path("priTmplId").asText(tmpl.path("templateId").asText()))) {
                    continue;
                }
                Map<String, Object> data = new LinkedHashMap<>();
                for (var field : tmpl.path("data")) {
                    String key = field.path("key").asText();
                    String value = values.get(field.path("type").asText(""));
                    if (key.isBlank() || value == null) {
                        continue;
                    }
                    data.put(key, Map.of("value", value));
                }
                if (data.isEmpty()) {
                    log.warn("订阅消息模板 {} 没有匹配到任何字段，请检查模板字段类型", tmplId);
                    return null;
                }
                return data;
            }
            log.warn("未找到订阅消息模板: {}", tmplId);
            return null;
        } catch (Exception e) {
            log.warn("组装订阅消息 data 失败, tmplId={}", tmplId, e);
            return null;
        }
    }

    private void doSend(String openid, String tmplId, String page, Map<String, Object> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("touser", openid);
        payload.put("template_id", tmplId);
        payload.put("page", page);
        payload.put("data", data);
        try {
            String body = RestClient.create().post()
                    .uri(SEND_URL + "?access_token={token}", accessToken)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(payload))
                    .retrieve()
                    .body(String.class);
            var root = objectMapper.readTree(body);
            int errcode = root.path("errcode").asInt(0);
            if (errcode == 0) {
                log.info("订阅消息已发送: openid={}, tmplId={}", openid, tmplId);
            } else if (errcode == 43101) {
                // 用户未订阅 / 已拒收：一次性订阅授权消耗完也会报这个码，属正常现象
                log.info("订阅消息未送达（用户未订阅或授权已用尽）: openid={}, tmplId={}", openid, tmplId);
            } else {
                log.warn("订阅消息发送失败: errcode={}, errmsg={}, tmplId={}",
                        errcode, root.path("errmsg").asText(""), tmplId);
            }
        } catch (Exception e) {
            log.warn("订阅消息请求异常: openid={}, tmplId={}", openid, tmplId, e);
        }
    }

    private String getAccessToken(String appId, String secret) {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireAt) {
            return accessToken;
        }
        synchronized (this) {
            if (accessToken != null && System.currentTimeMillis() < tokenExpireAt) {
                return accessToken;
            }
            LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credential");
            form.add("appid", appId);
            form.add("secret", secret);
            String body = RestClient.create().post()
                    .uri(TOKEN_URL)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            try {
                var root = objectMapper.readTree(body);
                String token = root.path("access_token").asText("");
                if (token.isBlank()) {
                    throw new IllegalStateException("获取 access_token 失败: " + body);
                }
                accessToken = token;
                // 有效期 7200s，提前 200s 过期
                tokenExpireAt = System.currentTimeMillis() + (root.path("expires_in").asLong(7200) - 200) * 1000;
                return token;
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("解析 access_token 响应失败", e);
            }
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > THING_MAX ? value.substring(0, THING_MAX) : value;
    }

    private String format(LocalDateTime time) {
        return time == null ? TIME_FORMAT.format(LocalDateTime.now()) : TIME_FORMAT.format(time);
    }

    private String formatMoney(BigDecimal amount) {
        return "¥" + (amount == null ? "0.00" : amount.toPlainString());
    }
}
