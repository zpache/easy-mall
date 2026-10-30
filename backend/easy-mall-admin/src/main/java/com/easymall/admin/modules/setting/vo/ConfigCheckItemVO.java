package com.easymall.admin.modules.setting.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 支付参数自检项。
 */
@Data
@AllArgsConstructor
public class ConfigCheckItemVO {

    /** PASS 通过 / WARN 提醒 / FAIL 阻断 / SKIP 未启用可跳过 */
    public static final String PASS = "PASS";
    public static final String WARN = "WARN";
    public static final String FAIL = "FAIL";
    public static final String SKIP = "SKIP";

    /** 检查项标识 */
    private String key;

    /** 检查项名称 */
    private String label;

    /** 检查结果 */
    private String status;

    /** 结论说明 */
    private String message;

    /** 是否影响支付可用（true 表示 FAIL 时不允许启用微信支付） */
    private boolean required;
}
