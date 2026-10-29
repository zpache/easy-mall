package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商城装修主题：全局换肤配置，草稿 / 发布双快照，is_active 全局唯一。
 * <p>保存只写草稿列；「启用」发布快照并切换线上主题，小程序端只读已启用主题的 published 列。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dc_theme")
public class DecorateTheme extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 主题配置草稿 JSON */
    private String configJson;

    /** 已发布的主题配置快照（小程序读这列） */
    private String publishedJson;

    /** 是否线上主题：0-否 1-是（全局唯一） */
    private Integer isActive;

    private String remark;
}
