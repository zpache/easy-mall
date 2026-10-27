package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分类。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_category")
public class Category extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long parentId;

    private String icon;

    private Integer sort;

    private Integer status;
}
