package com.easymall.admin.modules.trade.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 运费管理详情：全局配置 + 区域规则列表。
 */
@Data
public class FreightDetailVO {

    private BigDecimal defaultFee;

    /** 全国满额包邮门槛，null 表示不启用 */
    private BigDecimal freeAmount;

    private Integer version;

    private List<FreightRuleVO> rules;
}
