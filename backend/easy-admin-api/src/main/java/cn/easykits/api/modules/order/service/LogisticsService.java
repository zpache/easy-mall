package cn.easykits.api.modules.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.easykits.api.common.exception.BusinessException;
import cn.easykits.api.entity.DeliveryCompany;
import cn.easykits.api.entity.MallOrder;
import cn.easykits.api.entity.SysConfig;
import cn.easykits.api.mapper.DeliveryCompanyMapper;
import cn.easykits.api.modules.order.vo.LogisticsTraceVO;
import cn.easykits.api.modules.setting.service.SysConfigService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 物流查询：快递100 实时查询（poll/query.do），与 admin 端同口径。
 * <p>
 * 1. 快递公司编码取订单上的快照（发货时由快递公司表解析写入）；
 * 2. 老订单没有快照时按公司名查「商城设置 → 快递公司」（60 秒缓存），仍未命中才调「单号识别」；
 * 3. 只有顺丰、EMS 这类公司在查询时会校验收件人手机号后四位，其余快递不传；
 * 4. 查询签名为 MD5(param + key + customer) 大写；每次调用消耗一次查询额度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsService {

    private static final String QUERY_URL = "https://poll.kuaidi100.com/poll/query.do";
    private static final String AUTONUMBER_URL = "https://www.kuaidi100.com/autonumber/autoComNum";

    /** 即使公司表里没配，这几个编码也必须带手机号后四位，否则查不到轨迹 */
    private static final Set<String> PHONE_REQUIRED_CODES = Set.of("shunfeng", "ems");

    private static final long CACHE_TTL_MS = 60_000L;

    private final SysConfigService sysConfigService;
    private final DeliveryCompanyMapper deliveryCompanyMapper;
    private final ObjectMapper objectMapper;

    /** 名称 → 快递公司，60 秒进程内缓存（名单几乎不变，避免每次查轨迹都查库） */
    private volatile Map<String, DeliveryCompany> companyCache = Map.of();
    private volatile long cacheExpireAt = 0L;

    /**
     * 查询本人订单的物流轨迹。
     */
    public LogisticsTraceVO trace(Long userId, MallOrder order) {
        if (order.getDeliveryNo() == null || order.getDeliveryNo().isBlank()) {
            throw new BusinessException("订单尚未发货，暂无物流信息");
        }

        String customer = sysConfigService.get(SysConfig.GROUP_LOGISTICS, "kuaidi100Customer", "").trim();
        String apiKey = sysConfigService.get(SysConfig.GROUP_LOGISTICS, "kuaidi100Key", "").trim();
        boolean enabled = sysConfigService.getBoolean(SysConfig.GROUP_LOGISTICS, "kuaidi100Enabled", false);
        if (!enabled || customer.isBlank() || apiKey.isBlank()) {
            throw new BusinessException("商家未开启物流查询");
        }

        String company = order.getDeliveryCompany() == null ? "" : order.getDeliveryCompany().trim();
        DeliveryCompany configured = resolveByName(company);
        String companyCode = order.getDeliveryCompanyCode();
        if (!StringUtils.hasText(companyCode)) {
            companyCode = configured == null ? null : configured.getCode();
        }
        if (!StringUtils.hasText(companyCode)) {
            // 自定义快递（未在快递公司表里维护），按单号自动识别
            companyCode = autoDetectCompany(order.getDeliveryNo());
        }

        boolean phoneRequired = configured != null
                ? Integer.valueOf(1).equals(configured.getPhoneRequired())
                : PHONE_REQUIRED_CODES.contains(companyCode);
        String phone = phoneRequired ? phoneTail(order.getReceiverPhone()) : "";

        return query(company, companyCode, order.getDeliveryNo(), phone, customer, apiKey);
    }

    /* ---------------- private ---------------- */

    private Map<String, DeliveryCompany> loadCompanyCache() {
        if (System.currentTimeMillis() < cacheExpireAt) {
            return companyCache;
        }
        List<DeliveryCompany> all = deliveryCompanyMapper.selectList(
                Wrappers.<DeliveryCompany>lambdaQuery().eq(DeliveryCompany::getStatus, 1));
        companyCache = all.stream().collect(Collectors.toMap(
                DeliveryCompany::getName, Function.identity(), (a, b) -> a));
        cacheExpireAt = System.currentTimeMillis() + CACHE_TTL_MS;
        return companyCache;
    }

    private DeliveryCompany resolveByName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return loadCompanyCache().get(name.trim());
    }

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
        throw new BusinessException("无法识别快递公司编码，请稍后重试或联系商家");
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

        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
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
