-- ============================================================
-- 管理端按钮级权限初始化
--
-- 背景：接口已统一使用 @PreAuthorize 校验操作权限。ADMIN 由代码兜底拥有全部权限；
--       非 ADMIN 角色仍以 sys_menu.auth_code + sys_role_menu 为准，因此需要为现有页面
--       补齐 button 类型菜单，否则页面可见但新增、编辑、保存等操作会返回 403。
--
-- 行为：
--   1. 按 auth_code 找到现有页面菜单，在其下创建 button 节点（可重复执行）。
--   2. 已拥有该页面权限的角色，自动获得页面下的操作按钮权限；首次升级保持原使用范围。
--   3. ADMIN 角色补齐全部按钮授权，便于角色授权弹窗正确回显。
--   4. 已自行调整过授权的按钮不会被重复覆盖（仅当按钮尚无任何角色授权时自动分配）。
--
-- 用法：
--   mysql -uroot -p easy_mall < backend/easy-mall-admin/src/main/resources/permission-buttons.sql
-- ============================================================

DROP TABLE IF EXISTS `tmp_permission_button`;
CREATE TABLE `tmp_permission_button`
(
    `name`             varchar(50)  NOT NULL PRIMARY KEY,
    `title`            varchar(50)  NOT NULL,
    `auth_code`        varchar(100) NOT NULL,
    `parent_auth_code` varchar(100) NOT NULL,
    `order_no`         int          NOT NULL DEFAULT 0
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO `tmp_permission_button` (`name`, `title`, `auth_code`, `parent_auth_code`, `order_no`)
VALUES
-- 系统管理
('PermBtnSystemUserCreate', '新增用户', 'system:user:create', 'system:user:list', 10),
('PermBtnSystemUserUpdate', '编辑用户', 'system:user:update', 'system:user:list', 20),
('PermBtnSystemUserDelete', '删除用户', 'system:user:delete', 'system:user:list', 30),
('PermBtnSystemUserStatus', '启用停用用户', 'system:user:status', 'system:user:list', 40),
('PermBtnSystemUserResetPassword', '重置密码', 'system:user:reset-password', 'system:user:list', 50),
('PermBtnSystemRoleCreate', '新增角色', 'system:role:create', 'system:role:list', 10),
('PermBtnSystemRoleUpdate', '编辑角色', 'system:role:update', 'system:role:list', 20),
('PermBtnSystemRoleDelete', '删除角色', 'system:role:delete', 'system:role:list', 30),
('PermBtnSystemRoleStatus', '启用停用角色', 'system:role:status', 'system:role:list', 40),
('PermBtnSystemMenuCreate', '新增菜单', 'system:menu:create', 'system:menu:list', 10),
('PermBtnSystemMenuUpdate', '编辑菜单', 'system:menu:update', 'system:menu:list', 20),
('PermBtnSystemMenuDelete', '删除菜单', 'system:menu:delete', 'system:menu:list', 30),
('PermBtnSystemDeptCreate', '新增部门', 'system:dept:create', 'system:dept:list', 10),
('PermBtnSystemDeptUpdate', '编辑部门', 'system:dept:update', 'system:dept:list', 20),
('PermBtnSystemDeptDelete', '删除部门', 'system:dept:delete', 'system:dept:list', 30),
('PermBtnSystemMessageRead', '消息标为已读', 'system:message:read', 'system:message:list', 10),
('PermBtnSystemMessageReadAll', '消息全部已读', 'system:message:read-all', 'system:message:list', 20),

-- 商品管理
('PermBtnGoodsCreate', '新增商品', 'goods:goods:create', 'goods:goods:list', 10),
('PermBtnGoodsUpdate', '编辑商品', 'goods:goods:update', 'goods:goods:list', 20),
('PermBtnGoodsDelete', '删除商品', 'goods:goods:delete', 'goods:goods:list', 30),
('PermBtnGoodsStatus', '商品上下架', 'goods:goods:status', 'goods:goods:list', 40),
('PermBtnGoodsSku', 'SKU 配置', 'goods:goods:sku', 'goods:goods:list', 50),
('PermBtnGoodsCategoryCreate', '新增分类', 'goods:category:create', 'goods:category:list', 10),
('PermBtnGoodsCategoryUpdate', '编辑分类', 'goods:category:update', 'goods:category:list', 20),
('PermBtnGoodsCategoryDelete', '删除分类', 'goods:category:delete', 'goods:category:list', 30),
('PermBtnGoodsBrandCreate', '新增品牌', 'goods:brand:create', 'goods:brand:list', 10),
('PermBtnGoodsBrandUpdate', '编辑品牌', 'goods:brand:update', 'goods:brand:list', 20),
('PermBtnGoodsBrandDelete', '删除品牌', 'goods:brand:delete', 'goods:brand:list', 30),
('PermBtnGoodsBrandStatus', '启用停用品牌', 'goods:brand:status', 'goods:brand:list', 40),
('PermBtnGoodsStockAdjust', '库存调整', 'goods:stock:adjust', 'goods:stock:list', 10),
('PermBtnGoodsStockLogs', '库存流水', 'goods:stock:logs', 'goods:stock:list', 20),
('PermBtnGoodsStockImport', '批量导入库存', 'goods:stock:import', 'goods:stock:list', 30),

-- 订单 / 售后 / 评价
('PermBtnOrderDetail', '订单详情', 'order:order:detail', 'order:order:list', 10),
('PermBtnOrderClose', '关闭订单', 'order:order:close', 'order:order:list', 20),
('PermBtnOrderExport', '导出订单', 'order:order:export', 'order:order:list', 30),
('PermBtnOrderLogistics', '物流轨迹', 'order:order:logistics', 'order:order:list', 40),
('PermBtnOrderShip', '订单发货', 'order:order:ship', 'order:order:deliver', 10),
('PermBtnOrderBatchShip', '批量发货', 'order:order:batch-ship', 'order:order:deliver', 20),
('PermBtnOrderRefundAudit', '售后审核', 'order:refund:audit', 'order:refund:list', 10),
('PermBtnOrderRefundConfirm', '确认退款', 'order:refund:confirm', 'order:refund:list', 20),
('PermBtnOrderCommentAudit', '评价审核', 'order:comment:audit', 'order:comment:list', 10),
('PermBtnOrderCommentVisible', '评价显示隐藏', 'order:comment:visible', 'order:comment:list', 20),
('PermBtnOrderCommentReply', '评价回复', 'order:comment:reply', 'order:comment:list', 30),
('PermBtnOrderCommentDelete', '删除评价', 'order:comment:delete', 'order:comment:list', 40),

-- 会员管理
('PermBtnMemberPoints', '调整会员积分', 'member:member:points', 'member:member:list', 10),
('PermBtnMemberTag', '设置会员标签', 'member:member:tag', 'member:member:list', 20),
('PermBtnMemberLevelCreate', '新增等级', 'member:level:create', 'member:level:list', 10),
('PermBtnMemberLevelUpdate', '编辑等级', 'member:level:update', 'member:level:list', 20),
('PermBtnMemberLevelDelete', '删除等级', 'member:level:delete', 'member:level:list', 30),
('PermBtnMemberLevelStatus', '启用停用等级', 'member:level:status', 'member:level:list', 40),
('PermBtnMemberLevelDiscount', '等级折扣开关', 'member:level:discount', 'member:level:list', 50),
('PermBtnMemberPointsUpdate', '积分规则设置', 'member:points:update', 'member:points:list', 10),
('PermBtnMemberTagCreate', '新增标签', 'member:tag:create', 'member:tag:list', 10),
('PermBtnMemberTagUpdate', '编辑标签', 'member:tag:update', 'member:tag:list', 20),
('PermBtnMemberTagDelete', '删除标签', 'member:tag:delete', 'member:tag:list', 30),

-- 营销中心
('PermBtnMarketingCouponCreate', '新增优惠券', 'marketing:coupon:create', 'marketing:coupon:list', 10),
('PermBtnMarketingCouponUpdate', '编辑优惠券', 'marketing:coupon:update', 'marketing:coupon:list', 20),
('PermBtnMarketingCouponDelete', '删除优惠券', 'marketing:coupon:delete', 'marketing:coupon:list', 30),
('PermBtnMarketingCouponStatus', '启用停用优惠券', 'marketing:coupon:status', 'marketing:coupon:list', 40),
('PermBtnMarketingCouponGrant', '发放优惠券', 'marketing:coupon:grant', 'marketing:coupon:list', 50),
('PermBtnMarketingCouponRecords', '领取核销记录', 'marketing:coupon:records', 'marketing:coupon:list', 60),
('PermBtnMarketingPromotionCreate', '新增活动', 'marketing:promotion:create', 'marketing:promotion:list', 10),
('PermBtnMarketingPromotionUpdate', '编辑活动', 'marketing:promotion:update', 'marketing:promotion:list', 20),
('PermBtnMarketingPromotionDelete', '删除活动', 'marketing:promotion:delete', 'marketing:promotion:list', 30),
('PermBtnMarketingPromotionStatus', '启用停用活动', 'marketing:promotion:status', 'marketing:promotion:list', 40),
('PermBtnMarketingReductionCreate', '新增满减', 'marketing:reduction:create', 'marketing:reduction:list', 10),
('PermBtnMarketingReductionUpdate', '编辑满减', 'marketing:reduction:update', 'marketing:reduction:list', 20),
('PermBtnMarketingReductionDelete', '删除满减', 'marketing:reduction:delete', 'marketing:reduction:list', 30),
('PermBtnMarketingReductionStatus', '启用停用满减', 'marketing:reduction:status', 'marketing:reduction:list', 40),
('PermBtnMarketingSigninCreate', '新增签到规则', 'marketing:signin:create', 'marketing:signin:list', 10),
('PermBtnMarketingSigninUpdate', '编辑签到规则', 'marketing:signin:update', 'marketing:signin:list', 20),
('PermBtnMarketingSigninDelete', '删除签到规则', 'marketing:signin:delete', 'marketing:signin:list', 30),
('PermBtnMarketingSigninStatus', '启用停用签到', 'marketing:signin:status', 'marketing:signin:list', 40),
('PermBtnMarketingSigninLogs', '签到记录', 'marketing:signin:logs', 'marketing:signin:list', 50),

-- 商城装修
('PermBtnDecoratePageCreate', '新增页面', 'decorate:page:create', 'decorate:page:list', 10),
('PermBtnDecoratePageUpdate', '编辑页面', 'decorate:page:update', 'decorate:page:list', 20),
('PermBtnDecoratePagePublish', '发布页面', 'decorate:page:publish', 'decorate:page:list', 30),
('PermBtnDecoratePageCopy', '复制页面', 'decorate:page:copy', 'decorate:page:list', 40),
('PermBtnDecoratePageHome', '设为首页', 'decorate:page:home', 'decorate:page:list', 50),
('PermBtnDecoratePageDelete', '删除页面', 'decorate:page:delete', 'decorate:page:list', 60),
('PermBtnDecorateNavUpdate', '保存导航', 'decorate:nav:update', 'decorate:nav:list', 10),
('PermBtnDecorateNavPublish', '发布导航', 'decorate:nav:publish', 'decorate:nav:list', 20),
('PermBtnDecorateNavReset', '恢复默认导航', 'decorate:nav:reset', 'decorate:nav:list', 30),
('PermBtnDecorateMaterialUpload', '上传素材', 'decorate:material:upload', 'decorate:material:list', 10),
('PermBtnDecorateMaterialUpdate', '编辑素材', 'decorate:material:update', 'decorate:material:list', 20),
('PermBtnDecorateMaterialDelete', '删除素材', 'decorate:material:delete', 'decorate:material:list', 30),
('PermBtnDecorateThemeCreate', '新增主题', 'decorate:theme:create', 'decorate:theme:list', 10),
('PermBtnDecorateThemeUpdate', '编辑主题', 'decorate:theme:update', 'decorate:theme:list', 20),
('PermBtnDecorateThemeActivate', '启用主题', 'decorate:theme:activate', 'decorate:theme:list', 30),
('PermBtnDecorateThemeDelete', '删除主题', 'decorate:theme:delete', 'decorate:theme:list', 40),

-- 商城设置
('PermBtnTradeFreightUpdate', '保存运费配置', 'trade:freight:update', 'trade:freight:list', 10),
('PermBtnSettingsPaymentUpdate', '保存支付配置', 'settings:payment:update', 'settings:payment:list', 10),
('PermBtnSettingsPaymentCheck', '支付配置检测', 'settings:payment:check', 'settings:payment:list', 20),
('PermBtnSettingsLogisticsUpdate', '保存物流配置', 'settings:logistics:update', 'settings:logistics:list', 10),
('PermBtnSettingsNotifyUpdate', '保存通知配置', 'settings:notify:update', 'settings:notify:list', 10),
('PermBtnSettingsDeliveryCreate', '新增快递公司', 'settings:delivery:create', 'settings:delivery:list', 10),
('PermBtnSettingsDeliveryUpdate', '编辑快递公司', 'settings:delivery:update', 'settings:delivery:list', 20),
('PermBtnSettingsDeliveryDelete', '删除快递公司', 'settings:delivery:delete', 'settings:delivery:list', 30);

-- 在对应页面菜单下创建按钮节点；按 auth_code 取最早的一条页面菜单，避免重复父级产生重复按钮。
INSERT INTO `sys_menu` (`pid`, `name`, `title`, `type`, `auth_code`, `order_no`, `status`)
SELECT p.`id`, t.`name`, t.`title`, 'button', t.`auth_code`, 1000 + t.`order_no`, 1
FROM `tmp_permission_button` t
JOIN `sys_menu` p
  ON p.`id` = (SELECT MIN(p2.`id`)
               FROM `sys_menu` p2
               WHERE p2.`auth_code` = t.`parent_auth_code`
                 AND p2.`deleted` = 0)
ON DUPLICATE KEY UPDATE `pid`       = VALUES(`pid`),
                        `title`     = VALUES(`title`),
                        `auth_code` = VALUES(`auth_code`),
                        `order_no`  = VALUES(`order_no`),
                        `status`    = VALUES(`status`);

-- 首次升级：拥有页面权限的角色同步获得页面下的操作权限。
-- 已有任一角色授权的按钮视为已人工配置，不再自动追加，避免覆盖后续精细授权。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, b.`id`
FROM `sys_role_menu` rm
JOIN `sys_menu` p ON p.`id` = rm.`menu_id`
JOIN `tmp_permission_button` t ON t.`parent_auth_code` = p.`auth_code`
JOIN `sys_menu` b ON b.`name` = t.`name` AND b.`deleted` = 0
WHERE p.`deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` granted WHERE granted.`menu_id` = b.`id`);

-- ADMIN 默认拥有全部权限，同时补齐授权关系，便于角色授权弹窗正确回显。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`id`, b.`id`
FROM `sys_role` r
JOIN `sys_menu` b ON b.`name` LIKE 'PermBtn%' AND b.`deleted` = 0
WHERE r.`code` = 'ADMIN'
  AND r.`deleted` = 0
  AND NOT EXISTS (SELECT 1
                  FROM `sys_role_menu` granted
                  WHERE granted.`role_id` = r.`id`
                    AND granted.`menu_id` = b.`id`);

DROP TABLE IF EXISTS `tmp_permission_button`;
