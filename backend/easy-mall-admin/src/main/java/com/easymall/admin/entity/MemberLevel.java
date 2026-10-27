package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 会员等级与权益。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_member_level")
public class MemberLevel extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 折扣（百分比，100 表示无折扣） */
    private BigDecimal discount;

    /** 升级所需累计消费（元） */
    private Integer threshold;

    /** 权益说明 */
    private String description;

    private Integer sort;

    /** 1 启用，0 停用 */
    private Integer status;
}
