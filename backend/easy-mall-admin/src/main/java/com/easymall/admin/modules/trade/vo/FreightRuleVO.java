package com.easymall.admin.modules.trade.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 区域运费规则展示。
 */
@Data
public class FreightRuleVO {

    private Long id;

    private String regionCode;

    private String regionName;

    /** 1 省 2 市 */
    private Integer regionLevel;

    /** 展示用地区路径，如「广东省」或「广东省 / 深圳市」 */
    private String regionPath;

    private Integer deliverable;

    private BigDecimal fee;

    private BigDecimal freeAmount;

    private LocalDateTime updatedAt;

    /** 乐观锁版本号 */
    private Integer version;
}
