package com.easymall.admin.modules.setting.service;

import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.SysConfig;
import com.easymall.admin.modules.setting.dto.LogisticsConfigForm;
import com.easymall.admin.modules.setting.vo.LogisticsConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 物流配置：快递100 查询授权（customer + key）与开关。
 * <p>
 * 口径：key 为敏感项，落库后不回显明文；未配置 key 时开关允许保存但查询接口会明确提示，
 * 与支付配置「参数不全不允许开启」不同——物流查询失败不影响交易主链路。
 */
@Service
@RequiredArgsConstructor
public class LogisticsConfigService {

    public static final String MASK = "********";

    /** 前端传该值表示清空敏感项 */
    public static final String CLEAR = "__CLEAR__";

    private static final String GROUP = SysConfig.GROUP_LOGISTICS;
    private static final String KEY_ENABLED = "kuaidi100Enabled";
    private static final String KEY_CUSTOMER = "kuaidi100Customer";
    private static final String KEY_API_KEY = "kuaidi100Key";

    private final SysConfigService sysConfigService;

    /**
     * 配置详情（key 脱敏）。
     */
    public LogisticsConfigVO detail() {
        Map<String, String> map = sysConfigService.group(GROUP);
        LogisticsConfigVO vo = new LogisticsConfigVO();
        vo.setKuaidi100Enabled("true".equalsIgnoreCase(map.get(KEY_ENABLED)));
        vo.setKuaidi100Customer(map.getOrDefault(KEY_CUSTOMER, ""));

        String apiKey = map.getOrDefault(KEY_API_KEY, "");
        vo.setKuaidi100KeySet(!apiKey.isBlank());
        vo.setKuaidi100Key(apiKey.isBlank() ? "" : MASK);
        return vo;
    }

    /**
     * 保存配置。key 留空表示不修改，传 CLEAR 表示清空。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(LogisticsConfigForm form) {
        Map<String, String> current = sysConfigService.group(GROUP);

        boolean enabled = form.kuaidi100Enabled() != null
                ? form.kuaidi100Enabled()
                : "true".equalsIgnoreCase(current.get(KEY_ENABLED));
        String customer = form.kuaidi100Customer() == null
                ? current.getOrDefault(KEY_CUSTOMER, "").trim()
                : form.kuaidi100Customer().trim();
        String apiKey = mergeSecret(form.kuaidi100Key(), current.get(KEY_API_KEY));

        if (enabled && (customer.isBlank() || apiKey.isBlank())) {
            throw new BusinessException("启用物流查询前请先补全：快递100 客户编号与授权密钥");
        }

        sysConfigService.upsert(GROUP, KEY_ENABLED, String.valueOf(enabled));
        sysConfigService.upsert(GROUP, KEY_CUSTOMER, customer);
        sysConfigService.upsert(GROUP, KEY_API_KEY, apiKey);
    }

    private String mergeSecret(String incoming, String current) {
        if (incoming == null || incoming.isBlank() || MASK.equals(incoming)) {
            return current == null ? "" : current.trim();
        }
        if (CLEAR.equals(incoming)) {
            return "";
        }
        return incoming.trim();
    }
}
