package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 导航装修配置（只读）：单例记录 id=1，三段 JSON 描述 TabBar / 顶部导航 / 悬浮球。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dc_nav")
public class DecorateNav extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tabbarJson;

    private String navbarJson;

    private String floatJson;

    private String remark;
}
