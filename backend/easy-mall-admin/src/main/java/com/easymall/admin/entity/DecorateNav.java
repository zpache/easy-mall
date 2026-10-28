package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商城导航装修配置：单例记录（id=1），三段 JSON 分别描述底部 TabBar、顶部导航与悬浮快捷入口。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dc_nav")
public class DecorateNav extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 底部 TabBar：样式 + 3~5 个标签 */
    private String tabbarJson;

    /** 顶部导航：标题、样式（常规/沉浸式）、返回行为 */
    private String navbarJson;

    /** 快捷入口 / 悬浮球：客服、购物车、回到顶部 */
    private String floatJson;

    private String remark;
}
