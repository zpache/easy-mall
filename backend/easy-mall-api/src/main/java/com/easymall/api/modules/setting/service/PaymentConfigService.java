package com.easymall.api.modules.setting.service;

import com.easymall.api.entity.SysConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 支付配置读取：小程序侧只关心「哪个渠道可用」与「支付超时时长」，
 * 商户密钥等敏感参数不在这里读取（后续接入微信支付时再单独封装）。
 */
@Service
@RequiredArgsConstructor
public class PaymentConfigService {

    private static final String GROUP = SysConfig.GROUP_PAYMENT;
    private static final int DEFAULT_PAY_TIMEOUT = 30;
    private static final int DEFAULT_AUTO_RECEIVE_DAYS = 15;

    private final SysConfigService sysConfigService;

    /** 是否启用微信支付渠道 */
    public boolean wxpayEnabled() {
        return sysConfigService.getBoolean(GROUP, "wxpayEnabled", false);
    }

    /** 是否启用模拟支付；迁移脚本未执行时默认开启，保证旧环境仍可下单付款 */
    public boolean mockPayEnabled() {
        return sysConfigService.getBoolean(GROUP, "mockPayEnabled", true);
    }

    /** 订单支付超时时长（分钟） */
    public int orderPayTimeoutMinutes() {
        return sysConfigService.getInt(GROUP, "orderPayTimeoutMinutes", DEFAULT_PAY_TIMEOUT);
    }

    /** 发货后自动确认收货天数：0 表示不自动完成，需用户手动确认 */
    public int autoReceiveDays() {
        return sysConfigService.getInt(GROUP, "autoReceiveDays", DEFAULT_AUTO_RECEIVE_DAYS);
    }
}
