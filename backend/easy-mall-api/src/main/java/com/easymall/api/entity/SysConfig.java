package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置（只读）：与 admin 共用 sys_config 表，按「分组 + 键」读取。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    /** 支付分组 */
    public static final String GROUP_PAYMENT = "payment";

    /** 物流分组（快递100） */
    public static final String GROUP_LOGISTICS = "logistics";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupCode;

    private String configKey;

    private String configValue;

    private String valueType;

    private Integer isSecret;

    private String remark;
}
