package com.easymall.admin.modules.setting.service;

import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.SysConfig;
import com.easymall.admin.modules.setting.dto.PaymentConfigForm;
import com.easymall.admin.modules.setting.vo.ConfigCheckItemVO;
import com.easymall.admin.modules.setting.vo.PaymentConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 支付配置：渠道开关 + 微信支付 v3 商户参数 + 参数自检。
 *
 * 设计口径：
 * 1. 模拟支付（mock）与微信支付是两个独立渠道，至少启用一个，否则小程序无法付款；
 * 2. 启用微信支付时商户参数必须完整，参数不全不允许开启（避免线上出现「点支付必失败」）；
 * 3. 敏感项（APIv3 密钥 / 商户私钥）落库后不再回显明文，只返回脱敏值 + 是否已配置。
 */
@Service
@RequiredArgsConstructor
public class PaymentConfigService {

    /** 敏感值脱敏占位 */
    public static final String MASK = "********";

    /** 前端传该值表示清空敏感项 */
    public static final String CLEAR = "__CLEAR__";

    private static final String GROUP = SysConfig.GROUP_PAYMENT;
    private static final String KEY_WXPAY_ENABLED = "wxpayEnabled";
    private static final String KEY_MOCK_PAY_ENABLED = "mockPayEnabled";
    private static final String KEY_APP_ID = "appId";
    private static final String KEY_MCH_ID = "mchId";
    private static final String KEY_MCH_SERIAL_NO = "mchSerialNo";
    private static final String KEY_API_V3_KEY = "apiV3Key";
    private static final String KEY_PRIVATE_KEY = "privateKey";
    private static final String KEY_NOTIFY_URL = "notifyUrl";
    private static final String KEY_WXPAY_PUBLIC_KEY = "wxpayPublicKey";
    private static final String KEY_WXPAY_PUBLIC_KEY_ID = "wxpayPublicKeyId";
    private static final String KEY_REFUND_NOTIFY_URL = "refundNotifyUrl";
    private static final String KEY_PAY_TIMEOUT = "orderPayTimeoutMinutes";
    private static final String KEY_AUTO_RECEIVE_DAYS = "autoReceiveDays";

    private static final int DEFAULT_PAY_TIMEOUT = 30;
    private static final int DEFAULT_AUTO_RECEIVE_DAYS = 15;
    private static final int API_V3_KEY_LENGTH = 32;

    private final SysConfigService sysConfigService;

    /**
     * 支付配置详情（敏感项脱敏）。
     */
    public PaymentConfigVO detail() {
        Map<String, String> map = sysConfigService.group(GROUP);
        PaymentConfigVO vo = new PaymentConfigVO();
        vo.setWxpayEnabled("true".equalsIgnoreCase(map.get(KEY_WXPAY_ENABLED)));
        vo.setMockPayEnabled(!"false".equalsIgnoreCase(map.getOrDefault(KEY_MOCK_PAY_ENABLED, "true")));
        vo.setAppId(map.getOrDefault(KEY_APP_ID, ""));
        vo.setMchId(map.getOrDefault(KEY_MCH_ID, ""));
        vo.setMchSerialNo(map.getOrDefault(KEY_MCH_SERIAL_NO, ""));
        vo.setNotifyUrl(map.getOrDefault(KEY_NOTIFY_URL, ""));
        vo.setWxpayPublicKeyId(map.getOrDefault(KEY_WXPAY_PUBLIC_KEY_ID, ""));
        vo.setRefundNotifyUrl(map.getOrDefault(KEY_REFUND_NOTIFY_URL, ""));
        vo.setOrderPayTimeoutMinutes(parseInt(map.get(KEY_PAY_TIMEOUT), DEFAULT_PAY_TIMEOUT));
        vo.setAutoReceiveDays(parseInt(map.get(KEY_AUTO_RECEIVE_DAYS), DEFAULT_AUTO_RECEIVE_DAYS));

        String apiV3Key = map.getOrDefault(KEY_API_V3_KEY, "");
        vo.setApiV3KeySet(!apiV3Key.isBlank());
        vo.setApiV3Key(apiV3Key.isBlank() ? "" : MASK);

        String privateKey = map.getOrDefault(KEY_PRIVATE_KEY, "");
        vo.setPrivateKeySet(!privateKey.isBlank());
        vo.setPrivateKey(privateKey.isBlank() ? "" : MASK);

        String publicKey = map.getOrDefault(KEY_WXPAY_PUBLIC_KEY, "");
        vo.setWxpayPublicKeySet(!publicKey.isBlank());
        vo.setWxpayPublicKey(publicKey.isBlank() ? "" : MASK);
        return vo;
    }

    /**
     * 保存支付配置。敏感项留空表示不修改，传 CLEAR 表示清空。
     */
    public void update(PaymentConfigForm form) {
        Map<String, String> current = sysConfigService.group(GROUP);

        boolean wxpayEnabled = form.wxpayEnabled() != null
                ? form.wxpayEnabled()
                : "true".equalsIgnoreCase(current.get(KEY_WXPAY_ENABLED));
        boolean mockPayEnabled = form.mockPayEnabled() != null
                ? form.mockPayEnabled()
                : !"false".equalsIgnoreCase(current.getOrDefault(KEY_MOCK_PAY_ENABLED, "true"));

        if (!wxpayEnabled && !mockPayEnabled) {
            throw new BusinessException("至少需要启用一个支付渠道，否则用户无法付款");
        }

        String appId = trim(form.appId(), current.get(KEY_APP_ID));
        String mchId = trim(form.mchId(), current.get(KEY_MCH_ID));
        String mchSerialNo = trim(form.mchSerialNo(), current.get(KEY_MCH_SERIAL_NO));
        String notifyUrl = trim(form.notifyUrl(), current.get(KEY_NOTIFY_URL));
        String apiV3Key = mergeSecret(form.apiV3Key(), current.get(KEY_API_V3_KEY));
        String privateKey = mergeSecret(form.privateKey(), current.get(KEY_PRIVATE_KEY));
        String publicKey = mergeSecret(form.wxpayPublicKey(), current.get(KEY_WXPAY_PUBLIC_KEY));
        String publicKeyId = trim(form.wxpayPublicKeyId(), current.get(KEY_WXPAY_PUBLIC_KEY_ID));
        String refundNotifyUrl = trim(form.refundNotifyUrl(), current.get(KEY_REFUND_NOTIFY_URL));
        int payTimeout = form.orderPayTimeoutMinutes() == null
                ? parseInt(current.get(KEY_PAY_TIMEOUT), DEFAULT_PAY_TIMEOUT)
                : form.orderPayTimeoutMinutes();
        int autoReceiveDays = form.autoReceiveDays() == null
                ? parseInt(current.get(KEY_AUTO_RECEIVE_DAYS), DEFAULT_AUTO_RECEIVE_DAYS)
                : form.autoReceiveDays();

        if (wxpayEnabled) {
            List<String> missing = new ArrayList<>();
            if (appId.isBlank()) {
                missing.add("小程序 AppID");
            }
            if (mchId.isBlank()) {
                missing.add("商户号");
            }
            if (mchSerialNo.isBlank()) {
                missing.add("商户证书序列号");
            }
            if (apiV3Key.isBlank()) {
                missing.add("APIv3 密钥");
            }
            if (privateKey.isBlank()) {
                missing.add("商户 API 私钥");
            }
            if (notifyUrl.isBlank()) {
                missing.add("支付回调地址");
            }
            if (publicKey.isBlank()) {
                missing.add("微信支付公钥");
            }
            if (!missing.isEmpty()) {
                throw new BusinessException("启用微信支付前请先补全：" + String.join("、", missing));
            }
            if (apiV3Key.length() != API_V3_KEY_LENGTH) {
                throw new BusinessException("APIv3 密钥必须是 32 位字符");
            }
            if (!notifyUrl.toLowerCase().startsWith("https://")) {
                throw new BusinessException("支付回调地址必须是 https 地址");
            }
            if (!publicKey.contains("BEGIN") || !publicKey.contains("PUBLIC KEY")) {
                throw new BusinessException("微信支付公钥格式不正确，请粘贴 pub_key.pem 的完整内容（含 BEGIN/END 行）");
            }
        }

        sysConfigService.upsert(GROUP, KEY_WXPAY_ENABLED, String.valueOf(wxpayEnabled));
        sysConfigService.upsert(GROUP, KEY_MOCK_PAY_ENABLED, String.valueOf(mockPayEnabled));
        sysConfigService.upsert(GROUP, KEY_APP_ID, appId);
        sysConfigService.upsert(GROUP, KEY_MCH_ID, mchId);
        sysConfigService.upsert(GROUP, KEY_MCH_SERIAL_NO, mchSerialNo);
        sysConfigService.upsert(GROUP, KEY_API_V3_KEY, apiV3Key);
        sysConfigService.upsert(GROUP, KEY_PRIVATE_KEY, privateKey);
        sysConfigService.upsert(GROUP, KEY_NOTIFY_URL, notifyUrl);
        sysConfigService.upsert(GROUP, KEY_WXPAY_PUBLIC_KEY, publicKey);
        sysConfigService.upsert(GROUP, KEY_WXPAY_PUBLIC_KEY_ID, publicKeyId);
        sysConfigService.upsert(GROUP, KEY_REFUND_NOTIFY_URL, refundNotifyUrl);
        sysConfigService.upsert(GROUP, KEY_PAY_TIMEOUT, String.valueOf(payTimeout));
        sysConfigService.upsert(GROUP, KEY_AUTO_RECEIVE_DAYS, String.valueOf(autoReceiveDays));
    }

    /**
     * 参数自检：不落库，只按当前已保存的配置逐项校验，给运营一份可读的检查清单。
     */
    public List<ConfigCheckItemVO> check() {
        Map<String, String> map = sysConfigService.group(GROUP);
        boolean wxpayEnabled = "true".equalsIgnoreCase(map.get(KEY_WXPAY_ENABLED));
        boolean mockPayEnabled = !"false".equalsIgnoreCase(map.getOrDefault(KEY_MOCK_PAY_ENABLED, "true"));
        String appId = map.getOrDefault(KEY_APP_ID, "").trim();
        String mchId = map.getOrDefault(KEY_MCH_ID, "").trim();
        String mchSerialNo = map.getOrDefault(KEY_MCH_SERIAL_NO, "").trim();
        String apiV3Key = map.getOrDefault(KEY_API_V3_KEY, "").trim();
        String privateKey = map.getOrDefault(KEY_PRIVATE_KEY, "").trim();
        String notifyUrl = map.getOrDefault(KEY_NOTIFY_URL, "").trim();
        String publicKey = map.getOrDefault(KEY_WXPAY_PUBLIC_KEY, "").trim();
        String publicKeyId = map.getOrDefault(KEY_WXPAY_PUBLIC_KEY_ID, "").trim();

        List<ConfigCheckItemVO> items = new ArrayList<>();

        // 1. 渠道
        if (!wxpayEnabled && !mockPayEnabled) {
            items.add(new ConfigCheckItemVO("channel", "支付渠道", ConfigCheckItemVO.FAIL,
                    "模拟支付与微信支付都已关闭，用户无法付款", true));
        } else if (wxpayEnabled && mockPayEnabled) {
            items.add(new ConfigCheckItemVO("channel", "支付渠道", ConfigCheckItemVO.WARN,
                    "两个渠道同时开启：正式环境建议关闭模拟支付，仅保留微信支付", false));
        } else if (wxpayEnabled) {
            items.add(new ConfigCheckItemVO("channel", "支付渠道", ConfigCheckItemVO.PASS, "已启用微信支付", false));
        } else {
            items.add(new ConfigCheckItemVO("channel", "支付渠道", ConfigCheckItemVO.PASS,
                    "当前为模拟支付模式，仅可用于开发联调，请勿用于正式收款", false));
        }

        // 2. 小程序 AppID
        items.add(checkText("appId", "小程序 AppID", appId, wxpayEnabled,
                value -> value.startsWith("wx") && value.length() >= 16,
                "已配置", "未配置", "格式异常：小程序 AppID 以 wx 开头且长度 18 位左右"));

        // 3. 商户号
        items.add(checkText("mchId", "微信支付商户号", mchId, wxpayEnabled,
                value -> value.matches("\\d{8,10}"),
                "已配置", "未配置", "格式异常：商户号为 8~10 位数字"));

        // 4. 证书序列号
        items.add(checkText("mchSerialNo", "商户证书序列号", mchSerialNo, wxpayEnabled,
                value -> value.matches("[0-9A-Fa-f]{16,64}"),
                "已配置", "未配置", "格式异常：证书序列号为 16~64 位十六进制字符串"));

        // 5. APIv3 密钥
        if (!wxpayEnabled && apiV3Key.isBlank()) {
            items.add(new ConfigCheckItemVO("apiV3Key", "APIv3 密钥", ConfigCheckItemVO.SKIP,
                    "微信支付未启用，可暂不配置", true));
        } else if (apiV3Key.isBlank()) {
            items.add(new ConfigCheckItemVO("apiV3Key", "APIv3 密钥", ConfigCheckItemVO.FAIL,
                    "未配置：回调报文解密与退款需要 APIv3 密钥", true));
        } else if (apiV3Key.length() != API_V3_KEY_LENGTH) {
            items.add(new ConfigCheckItemVO("apiV3Key", "APIv3 密钥", ConfigCheckItemVO.FAIL,
                    "长度异常：APIv3 密钥固定 32 位，当前 " + apiV3Key.length() + " 位", true));
        } else {
            items.add(new ConfigCheckItemVO("apiV3Key", "APIv3 密钥", ConfigCheckItemVO.PASS,
                    "已配置（32 位）", true));
        }

        // 6. 商户私钥
        if (!wxpayEnabled && privateKey.isBlank()) {
            items.add(new ConfigCheckItemVO("privateKey", "商户 API 私钥", ConfigCheckItemVO.SKIP,
                    "微信支付未启用，可暂不配置", true));
        } else if (privateKey.isBlank()) {
            items.add(new ConfigCheckItemVO("privateKey", "商户 API 私钥", ConfigCheckItemVO.FAIL,
                    "未配置：下单签名必须使用商户私钥", true));
        } else if (!privateKey.contains("BEGIN") || !privateKey.contains("PRIVATE KEY")) {
            items.add(new ConfigCheckItemVO("privateKey", "商户 API 私钥", ConfigCheckItemVO.FAIL,
                    "格式异常：请粘贴 apiclient_key.pem 的完整内容（含 BEGIN/END 行）", true));
        } else {
            items.add(new ConfigCheckItemVO("privateKey", "商户 API 私钥", ConfigCheckItemVO.PASS, "已配置", true));
        }

        // 7. 回调地址
        if (!wxpayEnabled && notifyUrl.isBlank()) {
            items.add(new ConfigCheckItemVO("notifyUrl", "支付回调地址", ConfigCheckItemVO.SKIP,
                    "微信支付未启用，可暂不配置", true));
        } else if (notifyUrl.isBlank()) {
            items.add(new ConfigCheckItemVO("notifyUrl", "支付回调地址", ConfigCheckItemVO.FAIL,
                    "未配置：微信支付结果需要公网可访问的 https 回调地址", true));
        } else if (!notifyUrl.toLowerCase().startsWith("https://")) {
            items.add(new ConfigCheckItemVO("notifyUrl", "支付回调地址", ConfigCheckItemVO.FAIL,
                    "地址异常：微信支付只回调 https 地址", true));
        } else if (notifyUrl.contains("localhost") || notifyUrl.contains("127.0.0.1")) {
            items.add(new ConfigCheckItemVO("notifyUrl", "支付回调地址", ConfigCheckItemVO.FAIL,
                    "地址异常：回调地址必须公网可访问，不能是本地地址", true));
        } else {
            items.add(new ConfigCheckItemVO("notifyUrl", "支付回调地址", ConfigCheckItemVO.PASS, "已配置", true));
        }

        // 8. 微信支付公钥（回调验签）
        if (!wxpayEnabled && publicKey.isBlank()) {
            items.add(new ConfigCheckItemVO("wxpayPublicKey", "微信支付公钥", ConfigCheckItemVO.SKIP,
                    "微信支付未启用，可暂不配置", true));
        } else if (publicKey.isBlank()) {
            items.add(new ConfigCheckItemVO("wxpayPublicKey", "微信支付公钥", ConfigCheckItemVO.FAIL,
                    "未配置：支付回调验签需要微信支付公钥，缺失时回调一律被拒绝，订单无法入账", true));
        } else if (!publicKey.contains("BEGIN") || !publicKey.contains("PUBLIC KEY")) {
            items.add(new ConfigCheckItemVO("wxpayPublicKey", "微信支付公钥", ConfigCheckItemVO.FAIL,
                    "格式异常：请粘贴 pub_key.pem 的完整内容（含 BEGIN/END 行）", true));
        } else {
            items.add(new ConfigCheckItemVO("wxpayPublicKey", "微信支付公钥", ConfigCheckItemVO.PASS, "已配置", true));
        }

        // 9. 公钥 ID
        if (!wxpayEnabled && publicKeyId.isBlank()) {
            items.add(new ConfigCheckItemVO("wxpayPublicKeyId", "微信支付公钥 ID", ConfigCheckItemVO.SKIP,
                    "微信支付未启用，可暂不配置", false));
        } else if (publicKeyId.isBlank()) {
            items.add(new ConfigCheckItemVO("wxpayPublicKeyId", "微信支付公钥 ID", ConfigCheckItemVO.WARN,
                    "未填写：不填不影响支付，但建议填写（PUB_KEY_ID_ 开头）便于排查", false));
        } else if (!publicKeyId.startsWith("PUB_KEY_ID_")) {
            items.add(new ConfigCheckItemVO("wxpayPublicKeyId", "微信支付公钥 ID", ConfigCheckItemVO.WARN,
                    "格式提示：公钥 ID 一般以 PUB_KEY_ID_ 开头", false));
        } else {
            items.add(new ConfigCheckItemVO("wxpayPublicKeyId", "微信支付公钥 ID", ConfigCheckItemVO.PASS,
                    "已配置", false));
        }

        // 10. 自动确认收货
        int autoReceiveDays = parseInt(map.get(KEY_AUTO_RECEIVE_DAYS), DEFAULT_AUTO_RECEIVE_DAYS);
        if (autoReceiveDays < 0 || autoReceiveDays > 90) {
            items.add(new ConfigCheckItemVO("autoReceiveDays", "自动确认收货", ConfigCheckItemVO.FAIL,
                    "取值异常：自动确认收货天数应为 0~90 天", false));
        } else if (autoReceiveDays == 0) {
            items.add(new ConfigCheckItemVO("autoReceiveDays", "自动确认收货", ConfigCheckItemVO.WARN,
                    "已关闭自动确认收货：订单需用户手动确认才会变成已完成", false));
        } else {
            items.add(new ConfigCheckItemVO("autoReceiveDays", "自动确认收货", ConfigCheckItemVO.PASS,
                    "发货后 " + autoReceiveDays + " 天未确认将自动完成", false));
        }

        // 11. 支付超时
        int timeout = parseInt(map.get(KEY_PAY_TIMEOUT), DEFAULT_PAY_TIMEOUT);
        if (timeout < 1 || timeout > 1440) {
            items.add(new ConfigCheckItemVO("orderPayTimeoutMinutes", "支付超时关单", ConfigCheckItemVO.FAIL,
                    "取值异常：支付超时时间应为 1~1440 分钟", true));
        } else {
            items.add(new ConfigCheckItemVO("orderPayTimeoutMinutes", "支付超时关单", ConfigCheckItemVO.PASS,
                    timeout + " 分钟未支付将自动关单", false));
        }

        return items;
    }

    /* ---------------- private ---------------- */

    private ConfigCheckItemVO checkText(String key,
                                        String label,
                                        String value,
                                        boolean wxpayEnabled,
                                        java.util.function.Predicate<String> format,
                                        String passMessage,
                                        String emptyMessage,
                                        String badFormatMessage) {
        if (value.isBlank()) {
            return new ConfigCheckItemVO(key, label,
                    wxpayEnabled ? ConfigCheckItemVO.FAIL : ConfigCheckItemVO.SKIP,
                    wxpayEnabled ? emptyMessage : "微信支付未启用，可暂不配置", true);
        }
        if (!format.test(value)) {
            return new ConfigCheckItemVO(key, label, ConfigCheckItemVO.FAIL, badFormatMessage, true);
        }
        return new ConfigCheckItemVO(key, label, ConfigCheckItemVO.PASS, passMessage, true);
    }

    /** 表单值：null 表示不修改，CLEAR 表示清空，其余按新值处理 */
    private String mergeSecret(String incoming, String current) {
        if (incoming == null || incoming.isBlank() || MASK.equals(incoming)) {
            return current == null ? "" : current.trim();
        }
        if (CLEAR.equals(incoming)) {
            return "";
        }
        return incoming.trim();
    }

    private String trim(String incoming, String current) {
        if (incoming == null) {
            return current == null ? "" : current.trim();
        }
        return incoming.trim();
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
