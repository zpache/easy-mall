package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 导航装修配置（只读）：单例记录 id=1，只映射已发布的三段 JSON
 * （TabBar / 顶部导航 / 悬浮球）。草稿列不映射，发布是唯一生效闸门。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dc_nav")
public class DecorateNav extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tabbarPublishedJson;

    private String navbarPublishedJson;

    private String floatPublishedJson;

    private String remark;
}
