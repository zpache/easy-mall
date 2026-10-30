package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置：按「分组 + 键」存放的键值配置（支付 / 小程序 / 交易等）。
 * 所有值统一按字符串存储，读写时按 valueType 解析。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    /** 支付分组 */
    public static final String GROUP_PAYMENT = "payment";

    /** 物流分组（快递100） */
    public static final String GROUP_LOGISTICS = "logistics";

    /** 小程序分组 */
    public static final String GROUP_MINIPROGRAM = "miniprogram";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置分组：payment / miniprogram / trade */
    private String groupCode;

    /** 配置项键，分组内唯一 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 值类型：string / number / boolean / text */
    private String valueType;

    /** 1 敏感值（密钥、私钥），返回前端时脱敏 */
    private Integer isSecret;

    private String remark;
}
