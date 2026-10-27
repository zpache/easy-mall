package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门（树形结构，pid = 0 表示根部门）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上级部门 ID，0 为根 */
    private Long pid;

    private String name;

    private Integer sort;

    /** 1 正常，0 禁用 */
    private Integer status;

    private String remark;
}
