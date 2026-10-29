package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 商城装修主题（只读）：小程序端仅消费已启用主题的发布快照。
 */
@Data
@TableName("dc_theme")
public class DecorateTheme {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(select = false)
    private String name;

    /** 已发布的主题配置快照 */
    private String publishedJson;

    private Integer isActive;
}
