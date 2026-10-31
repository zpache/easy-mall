package com.easymall.admin.modules.setting.service;

import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.SysConfig;
import com.easymall.admin.modules.setting.dto.NotifyConfigForm;
import com.easymall.admin.modules.setting.vo.NotifyConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 通知配置：微信订阅消息的推送开关、小程序凭据与模板 ID。
 * <p>
 * 口径：secret 落库后不回显明文；启用开关要求 AppID + Secret 齐全；
 * 模板 ID 允许先留空（商家可能尚未在小程序后台申请模板），为空时对应推送静默跳过。
 */
@Service
@RequiredArgsConstructor
public class NotifyConfigService {

    public static final String MASK = "********";

    /** 前端传该值表示清空敏感项 */
    public static final String CLEAR = "__CLEAR__";

    private static final String GROUP = SysConfig.GROUP_NOTIFY;
    private static final String KEY_ENABLED = "subscribeEnabled";
    private static final String KEY_APP_ID = "wxAppId";
    private static final String KEY_APP_SECRET = "wxAppSecret";
    private static final String KEY_TMPL_DELIVER = "tmplDeliver";
    private static final String KEY_TMPL_REFUND = "tmplRefund";

    private final SysConfigService sysConfigService;

    /**
     * 配置详情（secret 脱敏）。
     */
    public NotifyConfigVO detail() {
        Map<String, String> map = sysConfigService.group(GROUP);
        NotifyConfigVO vo = new NotifyConfigVO();
        vo.setSubscribeEnabled("true".equalsIgnoreCase(map.get(KEY_ENABLED)));
        vo.setWxAppId(map.getOrDefault(KEY_APP_ID, ""));
        vo.setTmplDeliver(map.getOrDefault(KEY_TMPL_DELIVER, ""));
        vo.setTmplRefund(map.getOrDefault(KEY_TMPL_REFUND, ""));

        String secret = map.getOrDefault(KEY_APP_SECRET, "");
        vo.setWxAppSecretSet(!secret.isBlank());
        vo.setWxAppSecret(secret.isBlank() ? "" : MASK);
        return vo;
    }

    /**
     * 保存配置。secret 留空表示不修改，传 CLEAR 表示清空。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(NotifyConfigForm form) {
        Map<String, String> current = sysConfigService.group(GROUP);

        boolean enabled = form.subscribeEnabled() != null
                ? form.subscribeEnabled()
                : "true".equalsIgnoreCase(current.get(KEY_ENABLED));
        String appId = form.wxAppId() == null
                ? current.getOrDefault(KEY_APP_ID, "").trim()
                : form.wxAppId().trim();
        String secret = mergeSecret(form.wxAppSecret(), current.get(KEY_APP_SECRET));
        String tmplDeliver = form.tmplDeliver() == null
                ? current.getOrDefault(KEY_TMPL_DELIVER, "").trim()
                : form.tmplDeliver().trim();
        String tmplRefund = form.tmplRefund() == null
                ? current.getOrDefault(KEY_TMPL_REFUND, "").trim()
                : form.tmplRefund().trim();

        if (enabled && (appId.isBlank() || secret.isBlank())) {
            throw new BusinessException("启用消息推送前请先补全：小程序 AppID 与 AppSecret");
        }

        sysConfigService.upsert(GROUP, KEY_ENABLED, String.valueOf(enabled));
        sysConfigService.upsert(GROUP, KEY_APP_ID, appId);
        sysConfigService.upsert(GROUP, KEY_APP_SECRET, secret);
        sysConfigService.upsert(GROUP, KEY_TMPL_DELIVER, tmplDeliver);
        sysConfigService.upsert(GROUP, KEY_TMPL_REFUND, tmplRefund);
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
