package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品品牌。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_brand")
public class Brand extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 品牌 LOGO */
    private String logo;

    private Integer sort;

    /** 1 启用，0 停用 */
    private Integer status;

    private String remark;
}
