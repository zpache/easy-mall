package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 商品分类（二级结构，parentId = 0 表示一级分类）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_category")
public class Category extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long parentId;

    private Integer sort;

    /** 1 启用，0 停用 */
    private Integer status;

    /** 子分类，仅树形查询时填充，不落库 */
    @TableField(exist = false)
    private List<Category> children;
}
