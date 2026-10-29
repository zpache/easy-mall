package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商城导航装修配置：单例记录（id=1），三段 JSON 分别描述底部 TabBar、顶部导航与悬浮快捷入口。
 * <p>保存只写草稿列；点「发布」后拷贝到 published 列，小程序端只读 published 列。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dc_nav")
public class DecorateNav extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 底部 TabBar 草稿：样式 + 3~5 个标签 */
    private String tabbarJson;

    /** 已发布的底部 TabBar 快照（小程序读这列） */
    private String tabbarPublishedJson;

    /** 顶部导航草稿：标题、样式（常规/沉浸式）、返回行为 */
    private String navbarJson;

    /** 已发布的顶部导航快照（小程序读这列） */
    private String navbarPublishedJson;

    /** 快捷入口 / 悬浮球草稿：客服、购物车、回到顶部 */
    private String floatJson;

    /** 已发布的快捷入口快照（小程序读这列） */
    private String floatPublishedJson;

    private String remark;
}
