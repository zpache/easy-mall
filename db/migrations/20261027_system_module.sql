-- =============================================================
-- 增量脚本：系统管理模块（部门 / 角色 / 菜单 / 用户）
-- 背景：从 vben 演示项目迁移 system 模块，新增部门、角色、菜单及关联表，
--      并为 sys_user 补充 email、dept_id。
-- 用法：mysql -uroot -p easy_mall < db/migrations/20261027_system_module.sql
-- 说明：表使用 IF NOT EXISTS、数据使用 INSERT IGNORE，可重复执行。
-- =============================================================

-- ------------------------------------------------------------
-- 1. sys_user 增补字段
-- ------------------------------------------------------------
SET @col_exists := (SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'sys_user'
                      AND COLUMN_NAME = 'email');
SET @ddl := IF(@col_exists = 0,
               'ALTER TABLE `sys_user` ADD COLUMN `email` varchar(100) DEFAULT NULL COMMENT ''邮箱'' AFTER `phone`',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'sys_user'
                      AND COLUMN_NAME = 'dept_id');
SET @ddl := IF(@col_exists = 0,
               'ALTER TABLE `sys_user` ADD COLUMN `dept_id` bigint DEFAULT NULL COMMENT ''所属部门 ID'' AFTER `email`, ADD KEY `idx_sys_user_dept` (`dept_id`)',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2. 部门
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dept`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `pid`        bigint      NOT NULL DEFAULT 0 COMMENT '上级部门，0 为根',
    `name`       varchar(50) NOT NULL COMMENT '部门名称',
    `sort`       int         NOT NULL DEFAULT 0 COMMENT '排序值，越小越前',
    `status`     tinyint     NOT NULL DEFAULT 1 COMMENT '1 正常，0 禁用',
    `remark`     varchar(255)         DEFAULT NULL COMMENT '备注',
    `created_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` bigint               DEFAULT NULL COMMENT '创建人 ID',
    `updated_by` bigint               DEFAULT NULL COMMENT '最后修改人 ID',
    `version`    int         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`    tinyint     NOT NULL DEFAULT 0 COMMENT '0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sys_dept_pid` (`pid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='部门';

-- ------------------------------------------------------------
-- 3. 角色
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       varchar(50) NOT NULL COMMENT '角色名称',
    `code`       varchar(50) NOT NULL COMMENT '角色标识，如 ADMIN / OPERATOR',
    `sort`       int         NOT NULL DEFAULT 0 COMMENT '排序值，越小越前',
    `status`     tinyint     NOT NULL DEFAULT 1 COMMENT '1 正常，0 禁用',
    `remark`     varchar(255)         DEFAULT NULL COMMENT '备注',
    `created_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` bigint               DEFAULT NULL COMMENT '创建人 ID',
    `updated_by` bigint               DEFAULT NULL COMMENT '最后修改人 ID',
    `version`    int         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`    tinyint     NOT NULL DEFAULT 0 COMMENT '0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_code` (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色';

-- ------------------------------------------------------------
-- 4. 菜单 / 权限
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_menu`
(
    `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `pid`                   bigint       NOT NULL DEFAULT 0 COMMENT '上级菜单，0 为根',
    `name`                  varchar(50)  NOT NULL COMMENT '路由名称（唯一）',
    `title`                 varchar(50)  NOT NULL COMMENT '菜单标题',
    `type`                  varchar(20)  NOT NULL DEFAULT 'menu' COMMENT '类型：catalog/menu/button/link/embedded',
    `auth_code`             varchar(100)          DEFAULT NULL COMMENT '权限标识，如 system:user:add',
    `path`                  varchar(200)          DEFAULT NULL COMMENT '路由路径',
    `component`             varchar(200)          DEFAULT NULL COMMENT '组件路径',
    `icon`                  varchar(100)          DEFAULT NULL COMMENT '图标',
    `order_no`              int          NOT NULL DEFAULT 0 COMMENT '排序值，越小越前',
    `status`                tinyint      NOT NULL DEFAULT 1 COMMENT '1 正常，0 禁用',
    `badge`                 varchar(50)           DEFAULT NULL COMMENT '徽标内容',
    `badge_type`            varchar(20)           DEFAULT NULL COMMENT '徽标类型：dot/normal',
    `badge_variants`        varchar(20)           DEFAULT NULL COMMENT '徽标颜色',
    `keep_alive`            tinyint      NOT NULL DEFAULT 0 COMMENT '是否缓存页面',
    `affix_tab`             tinyint      NOT NULL DEFAULT 0 COMMENT '是否固定标签页',
    `hide_in_menu`          tinyint      NOT NULL DEFAULT 0 COMMENT '是否在菜单中隐藏',
    `hide_children_in_menu` tinyint      NOT NULL DEFAULT 0 COMMENT '是否隐藏子菜单',
    `hide_in_breadcrumb`    tinyint      NOT NULL DEFAULT 0 COMMENT '是否在面包屑中隐藏',
    `hide_in_tab`           tinyint      NOT NULL DEFAULT 0 COMMENT '是否在标签页中隐藏',
    `link`                  varchar(255)          DEFAULT NULL COMMENT '外链地址',
    `iframe_src`            varchar(255)          DEFAULT NULL COMMENT '内嵌 iframe 地址',
    `created_at`            datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`            bigint                DEFAULT NULL COMMENT '创建人 ID',
    `updated_by`            bigint                DEFAULT NULL COMMENT '最后修改人 ID',
    `version`               int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`               tinyint      NOT NULL DEFAULT 0 COMMENT '0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_menu_name` (`name`),
    KEY `idx_sys_menu_pid` (`pid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='菜单权限';

-- ------------------------------------------------------------
-- 5. 关联表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role`
(
    `id`         bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    bigint   NOT NULL COMMENT '用户 ID',
    `role_id`    bigint   NOT NULL COMMENT '角色 ID',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` bigint            DEFAULT NULL COMMENT '创建人 ID',
    `updated_by` bigint            DEFAULT NULL COMMENT '最后修改人 ID',
    `version`    int      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`    tinyint  NOT NULL DEFAULT 0 COMMENT '0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_role` (`user_id`, `role_id`),
    KEY `idx_sys_user_role_role` (`role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联';

CREATE TABLE IF NOT EXISTS `sys_role_menu`
(
    `id`         bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id`    bigint   NOT NULL COMMENT '角色 ID',
    `menu_id`    bigint   NOT NULL COMMENT '菜单 ID',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` bigint            DEFAULT NULL COMMENT '创建人 ID',
    `updated_by` bigint            DEFAULT NULL COMMENT '最后修改人 ID',
    `version`    int      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`    tinyint  NOT NULL DEFAULT 0 COMMENT '0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_menu` (`role_id`, `menu_id`),
    KEY `idx_sys_role_menu_menu` (`menu_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色菜单关联';

-- ------------------------------------------------------------
-- 6. 初始化数据
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_dept` (`id`, `pid`, `name`, `sort`, `status`, `remark`)
VALUES (1, 0, '易商城总部', 1, 1, '根部门'),
       (2, 1, '电商事业部', 1, 1, '负责商城运营'),
       (3, 1, '技术部', 2, 1, '负责系统研发与维护'),
       (4, 1, '客服部', 3, 1, '负责售后与客户咨询');

INSERT IGNORE INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `remark`)
VALUES (1, '超级管理员', 'ADMIN', 1, 1, '拥有全部权限'),
       (2, '运营人员', 'OPERATOR', 2, 1, '负责商品与订单运营');

INSERT IGNORE INTO `sys_menu` (`id`, `pid`, `name`, `title`, `type`, `auth_code`, `path`, `component`, `icon`, `order_no`,
                                `status`)
VALUES (1, 0, 'System', '系统管理', 'catalog', NULL, '/system', NULL, 'lucide:settings', 90, 1),
       (2, 1, 'SystemUser', '用户管理', 'menu', 'system:user:list', '/system/user', '/system/user/list', 'lucide:users',
        1, 1),
       (3, 1, 'SystemRole', '角色管理', 'menu', 'system:role:list', '/system/role', '/system/role/list',
        'lucide:shield-check', 2, 1),
       (4, 1, 'SystemMenu', '菜单管理', 'menu', 'system:menu:list', '/system/menu', '/system/menu/list', 'lucide:menu',
        3, 1),
       (5, 1, 'SystemDept', '部门管理', 'menu', 'system:dept:list', '/system/dept', '/system/dept/list',
        'lucide:building-2', 4, 1);

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
VALUES (1, 1);

-- 已有 admin 账号补齐部门与邮箱（不覆盖已有值）
UPDATE `sys_user`
SET `dept_id` = 1,
    `email`   = 'admin@easymall.local'
WHERE `username` = 'admin'
  AND (`dept_id` IS NULL OR `email` IS NULL);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 1),
       (1, 2),
       (1, 3),
       (1, 4),
       (1, 5);
