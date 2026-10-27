package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会员标签（分群基础）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("m_member_tag")
public class MemberTag extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 展示颜色 */
    private String color;

    private String remark;
}
