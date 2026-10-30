-- ============================================================
-- 运费管理模块：全局运费配置、区域运费规则、菜单与授权
-- 幂等：可重复执行
-- ============================================================

-- ------------------------------------------------------------
-- 1. 运费配置（全局单行，id 固定为 1）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `trade_freight_config`
(
    `id`          bigint         NOT NULL DEFAULT 1 COMMENT '主键，固定为 1 的单行配置',
    `default_fee` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '统一运费：未命中区域规则时按此金额收取',
    `free_amount` decimal(10, 2)          DEFAULT NULL COMMENT '全国满额包邮门槛（元），NULL 表示不启用；仅作用于按统一运费计费的地区',
    `remark`      varchar(255)            DEFAULT NULL COMMENT '备注',
    `created_at`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`  bigint                  DEFAULT NULL,
    `updated_by`  bigint                  DEFAULT NULL,
    `version`     int            NOT NULL DEFAULT 0,
    `deleted`     tinyint        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='运费配置（全局单行）';

-- ------------------------------------------------------------
-- 2. 区域运费规则（省 / 市两级，命中优先级：市 > 省 > 统一运费）
--    fee = 0 表示该地区无条件包邮；free_amount 满额包邮门槛，NULL 不启用
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `trade_freight_rule`
(
    `id`          bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `region_code` varchar(50)    NOT NULL COMMENT '行政区域编码（region.area_code）',
    `region_name` varchar(50)    NOT NULL COMMENT '行政区域名称（冗余，便于展示）',
    `region_level` tinyint       NOT NULL COMMENT '1 省 2 市',
    `deliverable` tinyint        NOT NULL DEFAULT 1 COMMENT '1 提供配送 0 不配送',
    `fee`         decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '固定运费（元），0 表示该地区无条件包邮',
    `free_amount` decimal(10, 2)          DEFAULT NULL COMMENT '满额包邮门槛（元），NULL 表示不启用',
    `created_at`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`  bigint                  DEFAULT NULL,
    `updated_by`  bigint                  DEFAULT NULL,
    `version`     int            NOT NULL DEFAULT 0,
    `deleted`     tinyint        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_fr_region` (`region_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='区域运费规则';

-- 初始配置行
INSERT IGNORE INTO `trade_freight_config` (`id`, `default_fee`, `free_amount`, `remark`)
VALUES (1, 0.00, NULL, '默认配置：全国统一免运费，未启用满额包邮');

-- ------------------------------------------------------------
-- 3. 菜单：商城设置（目录）+ 运费管理（子菜单）
--    order_no=50，位于商城装修(40)与系统管理(90)之间
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `pid`, `name`, `title`, `type`, `auth_code`, `path`, `component`, `icon`,
                                `order_no`, `status`)
VALUES (70, 0, 'Settings', '商城设置', 'catalog', NULL, '/settings', NULL, 'lucide:settings-2', 50, 1),
       (71, 70, 'SettingsFreight', '运费管理', 'menu', 'trade:freight:list', '/settings/freight',
        '/settings/freight/index', 'lucide:truck', 1, 1);

-- 超级管理员、运营人员均可见商城设置
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 70), (1, 71),
       (2, 70), (2, 71);
