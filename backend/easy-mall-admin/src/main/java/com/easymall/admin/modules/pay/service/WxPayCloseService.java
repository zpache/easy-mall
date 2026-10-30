package com.easymall.admin.modules.pay.service;

import com.easymall.admin.modules.pay.support.WxPaySigner;
import com.easymall.admin.modules.setting.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.security.SecureRandom;
import java.util.Map;

/**
 * 微信支付 v3 关单：后台关闭「待付款」订单时调用，关闭微信侧的预支付单。
 *
 * <p>若只关本地不关微信，用户停留在收银台时仍可能付款成功，那笔钱会变成
 * 「订单已取消、待人工退款」。因此后台关单成功后同步调用微信关单。</p>
 *
 * <p>best-effort：订单在微信侧不存在（用户从未拉起支付）或已支付时微信都会报错，
 * 这里只记日志不抛出，本地关单结果不受影响；已支付到账的场景由支付回调标记
 * 「待人工退款」兜底。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxPayCloseService {

    private static final String GROUP = "payment";
    private static final String API_BASE = "https://api.mch.weixin.qq.com";
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final SysConfigService sysConfigService;

    private final SecureRandom random = new SecureRandom();

    /**
     * 关闭微信侧预支付单。
     */
    public void close(String outTradeNo) {
        boolean wxpayEnabled = "true".equalsIgnoreCase(cfg("wxpayEnabled", "false"));
        if (!wxpayEnabled) {
            return;
        }
        String mchId = cfg("mchId", "");
        String serialNo = cfg("mchSerialNo", "");
        String privateKey = cfg("privateKey", "");
        if (!StringUtils.hasText(mchId) || !StringUtils.hasText(serialNo) || !StringUtils.hasText(privateKey)) {
            log.warn("微信支付参数未配置完整，跳过微信关单: outTradeNo={}", outTradeNo);
            return;
        }

        String path = "/v3/pay/transactions/out-trade-no/" + outTradeNo + "/close";
        String body = "{\"mchid\":\"" + mchId + "\"}";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = randomString(32);
        String message = "POST\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        String signature = WxPaySigner.sign(privateKey, message);
        String authorization = "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + mchId + "\","
                + "nonce_str=\"" + nonce + "\","
                + "signature=\"" + signature + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + serialNo + "\"";

        try {
            RestClient.create(API_BASE)
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", authorization)
                    .header("Accept", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("微信关单成功: outTradeNo={}", outTradeNo);
        } catch (Exception e) {
            log.warn("微信关单失败（不影响本地关单）: outTradeNo={}, err={}", outTradeNo, e.getMessage());
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
