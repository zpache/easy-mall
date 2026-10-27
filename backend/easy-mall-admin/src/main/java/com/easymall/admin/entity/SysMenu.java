package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单 / 权限。字段与 vben 的路由 + meta 结构对应，前端菜单管理页直接编辑。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上级菜单 ID，0 为根 */
    private Long pid;

    /** 路由名称，全局唯一 */
    private String name;

    /** 菜单标题 */
    private String title;

    /** 类型：catalog 目录 / menu 菜单 / button 按钮 / link 外链 / embedded 内嵌 */
    private String type;

    /** 权限标识，如 system:user:add */
    private String authCode;

    /** 路由路径 */
    private String path;

    /** 组件路径 */
    private String component;

    private String icon;

    /** 排序值，越小越前 */
    private Integer orderNo;

    /** 1 正常，0 禁用 */
    private Integer status;

    private String badge;

    private String badgeType;

    private String badgeVariants;

    /** 1 缓存页面 */
    private Integer keepAlive;

    /** 1 固定标签页 */
    private Integer affixTab;

    /** 1 在菜单中隐藏 */
    private Integer hideInMenu;

    /** 1 隐藏子菜单 */
    private Integer hideChildrenInMenu;

    /** 1 在面包屑中隐藏 */
    private Integer hideInBreadcrumb;

    /** 1 在标签页中隐藏 */
    private Integer hideInTab;

    /** 外链地址 */
    private String link;

    /** 内嵌 iframe 地址 */
    private String iframeSrc;
}
