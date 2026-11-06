package com.easymall.admin.common.permission;

import java.util.Set;

/**
 * 管理端内置权限码。
 * <p>
 * 菜单 / 按钮的授权关系仍以 sys_menu.auth_code + sys_role_menu 为准；这里集中登记代码中
 * 实际校验的权限码，用于超级管理员兜底和前端按钮展示。新增后端校验时必须同步登记，
 * 否则非 ADMIN 角色即使授权也无法通过 authority 校验，ADMIN 也不会默认获得该码。
 */
public final class PermissionCodes {

    public static final Set<String> ALL = Set.of(
            // 工作台
            "dashboard:analytics",

            // 系统管理
            "system:user:list", "system:user:create", "system:user:update",
            "system:user:delete", "system:user:status", "system:user:reset-password",
            "system:role:list", "system:role:create", "system:role:update",
            "system:role:delete", "system:role:status",
            "system:menu:list", "system:menu:create", "system:menu:update", "system:menu:delete",
            "system:dept:list", "system:dept:create", "system:dept:update", "system:dept:delete",
            "system:operlog:list", "system:loginlog:list",
            "system:message:list", "system:message:read", "system:message:read-all",

            // 商品
            "goods:goods:list", "goods:goods:create", "goods:goods:update",
            "goods:goods:delete", "goods:goods:status", "goods:goods:sku",
            "goods:category:list", "goods:category:create", "goods:category:update",
            "goods:category:delete",
            "goods:brand:list", "goods:brand:create", "goods:brand:update",
            "goods:brand:delete", "goods:brand:status",
            "goods:stock:list", "goods:stock:adjust", "goods:stock:logs", "goods:stock:import",

            // 订单 / 售后 / 评价
            "order:order:list", "order:order:detail", "order:order:deliver",
            "order:order:ship", "order:order:batch-ship", "order:order:close",
            "order:order:export", "order:order:logistics",
            "order:refund:list", "order:refund:audit", "order:refund:confirm",
            "order:comment:list", "order:comment:audit", "order:comment:visible",
            "order:comment:reply", "order:comment:delete",

            // 会员
            "member:member:list", "member:member:tag", "member:member:points",
            "member:level:list", "member:level:create", "member:level:update",
            "member:level:delete", "member:level:status", "member:level:discount",
            "member:points:list", "member:points:update",
            "member:tag:list", "member:tag:create", "member:tag:update", "member:tag:delete",
            "member:invite:list",

            // 营销
            "marketing:coupon:list", "marketing:coupon:create", "marketing:coupon:update",
            "marketing:coupon:delete", "marketing:coupon:status", "marketing:coupon:records",
            "marketing:coupon:grant",
            "marketing:promotion:list", "marketing:promotion:create", "marketing:promotion:update",
            "marketing:promotion:delete", "marketing:promotion:status",
            "marketing:reduction:list", "marketing:reduction:create", "marketing:reduction:update",
            "marketing:reduction:delete", "marketing:reduction:status",
            "marketing:signin:list", "marketing:signin:create", "marketing:signin:update",
            "marketing:signin:delete", "marketing:signin:status", "marketing:signin:logs",

            // 装修
            "decorate:page:list", "decorate:page:create", "decorate:page:update",
            "decorate:page:publish", "decorate:page:copy", "decorate:page:home",
            "decorate:page:delete",
            "decorate:nav:list", "decorate:nav:update", "decorate:nav:publish", "decorate:nav:reset",
            "decorate:material:list", "decorate:material:upload", "decorate:material:update",
            "decorate:material:delete",
            "decorate:theme:list", "decorate:theme:create", "decorate:theme:update",
            "decorate:theme:activate", "decorate:theme:delete",
            "file:upload",

            // 商城设置 / 运费
            "trade:freight:list", "trade:freight:update",
            "settings:payment:list", "settings:payment:update", "settings:payment:check",
            "settings:logistics:list", "settings:logistics:update",
            "settings:notify:list", "settings:notify:update",
            "settings:delivery:list", "settings:delivery:create", "settings:delivery:update",
            "settings:delivery:delete"
    );

    private PermissionCodes() {
    }
}
