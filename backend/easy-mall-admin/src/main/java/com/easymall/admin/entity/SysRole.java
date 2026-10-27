package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 角色标识，如 ADMIN / OPERATOR，用于 Spring Security 的 ROLE_ 前缀 */
    private String code;

    private Integer sort;

    /** 1 正常，0 禁用 */
    private Integer status;

    private String remark;
}
