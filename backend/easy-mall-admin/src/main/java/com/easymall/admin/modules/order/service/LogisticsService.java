package com.easymall.admin.modules.order.service;

import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.Order;
import com.easymall.admin.entity.SysConfig;
import com.easymall.admin.modules.order.vo.LogisticsTraceVO;
import com.easymall.admin.modules.setting.service.SysConfigService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 物流查询：快递100 实时查询（poll/query.do）。
 * <p>
 * 1. 快递公司名映射为快递100 公司编码（发货表单预设的常见快递）；
 * 2. 编码映射不到时调用快递100「单号识别」按单号自动判断；
 * 3. 查询签名为 MD5(param + key + customer) 大写；每次调用消耗一次查询额度，
 *    本期不做轨迹缓存，额度吃紧时再加落库缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsService {

    private static final String QUERY_URL = "https://poll.kuaidi100.com/poll/query.do";
    private static final String AUTONUMBER_URL = "https://www.kuaidi100.com/autonumber/autoComNum";

    /** 发货表单预设快递公司 → 快递100 公司编码 */
    private static final Map<String, String> COMPANY_CODES = Map.of(
            "顺丰速运", "shunfeng",
            "京东物流", "jd",
            "中通快递", "zhongtong",
            "圆通速递", "yuantong",
            "申通快递", "shentong",
            "韵达快递", "yunda",
            "邮政 EMS", "ems",
            "极兔速递", "jtexpress");

    private final SysConfigService sysConfigService;
    private final ObjectMapper objectMapper;

    /**
     * 查询订单物流轨迹。
     */
    public LogisticsTraceVO trace(Order order) {
        if (order.getDeliveryNo() == null || order.getDeliveryNo().isBlank()) {
            throw new BusinessException("订单尚未发货，无物流信息");
        }

        Map<String, String> config = sysConfigService.group(SysConfig.GROUP_LOGISTICS);
        boolean enabled = "true".equalsIgnoreCase(config.get("kuaidi100Enabled"));
        String customer = config.getOrDefault("kuaidi100Customer", "").trim();
        String apiKey = config.getOrDefault("kuaidi100Key", "").trim();
        if (!enabled || customer.isBlank() || apiKey.isBlank()) {
            throw new BusinessException("物流查询未开启，请先在商城设置-物流配置中填写快递100 授权信息");
        }

        String company = order.getDeliveryCompany() == null ? "" : order.getDeliveryCompany().trim();
        String companyCode = COMPANY_CODES.get(company);
        if (companyCode == null) {
            // 未命中预设映射（自定义快递公司），按单号自动识别
            companyCode = autoDetectCompany(order.getDeliveryNo());
        }

        return query(company, companyCode, order.getDeliveryNo(), phoneTail(order.getReceiverPhone()),
                customer, apiKey);
    }

    /* ---------------- private ---------------- */

    /** 顺丰等快递要求手机号后四位校验 */
    private String phoneTail(String phone) {
        if (phone == null || phone.length() < 4) {
            return "";
        }
        return phone.substring(phone.length() - 4);
    }

    /** 单号识别：返回置信度最高的公司编码 */
    private String autoDetectCompany(String number) {
        try {
            String body = RestClient.create().get()
                    .uri(AUTONUMBER_URL + "?text={num}", number)
                    .retrieve()
                    .body(String.class);
            JsonNode auto = objectMapper.readTree(body).path("auto");
            if (auto.isArray() && !auto.isEmpty()) {
                return auto.get(0).path("comCode").asText("");
            }
        } catch (Exception e) {
            log.warn("快递100 单号识别失败: num={}", number, e);
        }
        throw new BusinessException("无法识别快递公司编码，请检查快递公司名称或单号");
    }

    private LogisticsTraceVO query(String company, String companyCode, String number, String phone,
                                   String customer, String apiKey) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("com", companyCode);
        param.put("num", number);
        if (!phone.isBlank()) {
            param.put("phone", phone);
        }
        param.put("resultv2", "4");

        String paramJson;
        try {
            paramJson = objectMapper.writeValueAsString(param);
        } catch (Exception e) {
            throw new BusinessException("物流查询参数组装失败");
        }

        org.springframework.util.LinkedMultiValueMap<String, String> form =
                new org.springframework.util.LinkedMultiValueMap<>();
        form.add("customer", customer);
        form.add("sign", sign(paramJson, apiKey, customer));
        form.add("param", paramJson);

        String body;
        try {
            body = RestClient.create().post()
                    .uri(QUERY_URL)
                    .body(form)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("快递100 查询请求失败: num={}", number, e);
            throw new BusinessException("物流查询请求失败，请稍后重试");
        }

        return parse(company, companyCode, number, body);
    }

    private LogisticsTraceVO parse(String company, String companyCode, String number, String body) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("快递100 响应解析失败: body={}", body, e);
            throw new BusinessException("物流查询响应异常，请稍后重试");
        }

        String status = root.path("status").asText("");
        String message = root.path("message").asText("");
        if (!"200".equals(status)) {
            // 常见：单号不存在/未揽收、签名错误、查询次数用尽
            throw new BusinessException("物流查询失败：" + (message.isBlank() ? "status=" + status : message));
        }

        List<LogisticsTraceVO.Node> traces = new ArrayList<>();
        JsonNode data = root.path("data");
        if (data.isArray()) {
            for (JsonNode item : data) {
                traces.add(new LogisticsTraceVO.Node(
                        item.path("time").asText(""),
                        item.path("ftime").asText(""),
                        item.path("context").asText(""),
                        item.path("location").asText("")));
            }
        }
        return new LogisticsTraceVO(company, companyCode, number, status, message,
                root.path("ischeck").asText("0").equals("1"),
                root.path("state").asText(""), traces);
    }

    /** 快递100 签名：MD5(param + key + customer) 大写 */
    private String sign(String paramJson, String apiKey, String customer) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest((paramJson + apiKey + customer).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            throw new BusinessException("物流查询签名失败");
        }
    }
}
