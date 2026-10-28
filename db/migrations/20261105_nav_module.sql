-- ============================================================
-- 导航装修：全局导航配置（底部 TabBar / 顶部导航 / 快捷入口）
-- 幂等：可重复执行
-- 单例表：固定 id = 1，保存即生效
-- ============================================================

CREATE TABLE IF NOT EXISTS `dc_nav`
(
    `id`           bigint   NOT NULL AUTO_INCREMENT COMMENT '主键（固定 1，单例配置）',
    `tabbar_json`  longtext          DEFAULT NULL COMMENT '底部 TabBar：样式与 3~5 个标签',
    `navbar_json`  longtext          DEFAULT NULL COMMENT '顶部导航：标题、样式、返回行为',
    `float_json`   longtext          DEFAULT NULL COMMENT '快捷入口/悬浮球：客服、购物车、回到顶部',
    `remark`       varchar(255)      DEFAULT NULL COMMENT '备注',
    `created_at`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`   bigint            DEFAULT NULL,
    `updated_by`   bigint            DEFAULT NULL,
    `version`      int      NOT NULL DEFAULT 0,
    `deleted`      tinyint  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商城导航装修配置';

-- ------------------------------------------------------------
-- 菜单：商城装修 / 导航装修
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `pid`, `name`, `title`, `type`, `auth_code`, `path`, `component`, `icon`,
                                `order_no`, `status`)
VALUES (62, 60, 'DecorateNav', '导航装修', 'menu', 'decorate:nav:list', '/decorate/nav', '/decorate/nav/index',
        'lucide:smartphone', 2, 1);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 62), (2, 62);

-- ------------------------------------------------------------
-- 默认配置：3 个标签（首页 / 分类 / 购物车 / 我的）
-- 前端读取后即为所见即所得，改动走 PUT 覆盖
-- ------------------------------------------------------------
INSERT IGNORE INTO `dc_nav` (`id`, `tabbar_json`, `navbar_json`, `float_json`, `remark`)
VALUES (1,
        '{"bgColor":"#ffffff","borderStyle":"black","color":"#909399","enabled":true,"items":[{"icon":"","key":"home","linkType":"home","linkValue":"","name":"首页","selectedIcon":"","visible":true},{"icon":"","key":"category","linkType":"path","linkValue":"/pages/category/index","name":"分类","selectedIcon":"","visible":true},{"icon":"","key":"cart","linkType":"path","linkValue":"/pages/cart/index","name":"购物车","selectedIcon":"","visible":true},{"icon":"","key":"mine","linkType":"path","linkValue":"/pages/mine/index","name":"我的","selectedIcon":"","visible":true}],"selectedColor":"#1677ff"}',
        '{"backBehavior":"back","bgColor":"#ffffff","showBack":true,"style":"normal","textColor":"#333333","title":"易商城"}',
        '{"enabled":true,"itemList":[{"enabled":true,"icon":"","key":"service","name":"客服","value":"400-800-1234"},{"enabled":true,"icon":"","key":"cart","name":"购物车","value":""},{"enabled":true,"icon":"","key":"top","name":"回到顶部","value":""}],"position":"right"}',
        '默认导航配置');
