-- ============================================================
-- 会员管理模块：会员等级、积分流水、标签、会员渠道字段、菜单与授权
-- 幂等：可重复执行
-- ============================================================

-- ------------------------------------------------------------
-- 1. app_user 增补会员字段（动态 SQL 保证幂等）
-- ------------------------------------------------------------
SET @has_level_id := (SELECT COUNT(*)
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = DATABASE()
                        AND TABLE_NAME = 'app_user'
                        AND COLUMN_NAME = 'level_id');
SET @sql := IF(@has_level_id = 0,
               'ALTER TABLE `app_user` ADD COLUMN `level_id` bigint NOT NULL DEFAULT 1 COMMENT ''会员等级 ID'' AFTER `gender`',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_points := (SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'app_user'
                      AND COLUMN_NAME = 'points');
SET @sql := IF(@has_points = 0,
               'ALTER TABLE `app_user` ADD COLUMN `points` int NOT NULL DEFAULT 0 COMMENT ''当前积分'' AFTER `level_id`',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 来源渠道：0 公众号 / 1 小程序 / 2 H5 扫码
SET @has_source := (SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'app_user'
                      AND COLUMN_NAME = 'source');
SET @sql := IF(@has_source = 0,
               'ALTER TABLE `app_user` ADD COLUMN `source` tinyint NOT NULL DEFAULT 1 COMMENT ''0 公众号 1 小程序 2 H5 扫码'' AFTER `points`',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_total_spend := (SELECT COUNT(*)
                         FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE()
                           AND TABLE_NAME = 'app_user'
                           AND COLUMN_NAME = 'total_spend');
SET @sql := IF(@has_total_spend = 0,
               'ALTER TABLE `app_user` ADD COLUMN `total_spend` decimal(10, 2) NOT NULL DEFAULT 0 COMMENT ''累计消费金额'' AFTER `source`',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_order_count := (SELECT COUNT(*)
                         FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE()
                           AND TABLE_NAME = 'app_user'
                           AND COLUMN_NAME = 'order_count');
SET @sql := IF(@has_order_count = 0,
               'ALTER TABLE `app_user` ADD COLUMN `order_count` int NOT NULL DEFAULT 0 COMMENT ''累计订单数'' AFTER `total_spend`',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2. 会员等级
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mall_member_level`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(50)  NOT NULL COMMENT '等级名称',
    `discount`    decimal(4, 1) NOT NULL DEFAULT 100.0 COMMENT '折扣（百分比，100 表示无折扣）',
    `threshold`   int          NOT NULL DEFAULT 0 COMMENT '升级所需累计消费（元）',
    `description` varchar(500)          DEFAULT NULL COMMENT '权益说明',
    `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '1 启用，0 停用',
    `created_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`  bigint                DEFAULT NULL,
    `updated_by`  bigint                DEFAULT NULL,
    `version`     int          NOT NULL DEFAULT 0,
    `deleted`     tinyint      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_level_name` (`name`, `deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='会员等级与权益';

-- ------------------------------------------------------------
-- 3. 积分流水
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mall_points_log`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    bigint      NOT NULL COMMENT '会员 ID',
    `change`     int         NOT NULL COMMENT '积分变动量，正得负耗',
    `balance`    int         NOT NULL COMMENT '变动后余额',
    `type`       varchar(16) NOT NULL DEFAULT 'earn' COMMENT 'earn 消费获得 / consume 积分抵扣 / adjust 人工调整',
    `order_no`   varchar(32)          DEFAULT NULL COMMENT '关联订单号',
    `remark`     varchar(255)         DEFAULT NULL COMMENT '备注',
    `created_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by` bigint               DEFAULT NULL COMMENT '操作人 ID（人工调整时）',
    PRIMARY KEY (`id`),
    KEY `idx_points_user` (`user_id`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='会员积分流水';

-- ------------------------------------------------------------
-- 4. 会员标签与关联
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mall_member_tag`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       varchar(50) NOT NULL COMMENT '标签名称',
    `color`      varchar(20)          DEFAULT NULL COMMENT '展示颜色',
    `remark`     varchar(255)         DEFAULT NULL COMMENT '备注',
    `created_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` bigint               DEFAULT NULL,
    `updated_by` bigint               DEFAULT NULL,
    `version`    int         NOT NULL DEFAULT 0,
    `deleted`    tinyint     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`name`, `deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='会员标签';

CREATE TABLE IF NOT EXISTS `mall_member_user_tag`
(
    `id`      bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` bigint NOT NULL COMMENT '会员 ID',
    `tag_id`  bigint NOT NULL COMMENT '标签 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_tag` (`user_id`, `tag_id`),
    KEY `idx_user_tag_tag` (`tag_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='会员-标签关联';

-- ------------------------------------------------------------
-- 5. 菜单：会员管理（目录 + 4 个子菜单）
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `pid`, `name`, `title`, `type`, `auth_code`, `path`, `component`, `icon`,
                                `order_no`, `status`)
VALUES (40, 0, 'Member', '会员管理', 'catalog', NULL, '/member', NULL, 'lucide:users', 20, 1),
       (41, 40, 'MemberList', '会员列表', 'menu', 'member:member:list', '/member/list', '/member/list/index',
        'lucide:user-round', 1, 1),
       (42, 40, 'MemberLevel', '等级与权益', 'menu', 'member:level:list', '/member/level', '/member/level/index',
        'lucide:crown', 2, 1),
       (43, 40, 'MemberPoints', '积分管理', 'menu', 'member:points:list', '/member/points', '/member/points/index',
        'lucide:coins', 3, 1),
       (44, 40, 'MemberTag', '标签与分群', 'menu', 'member:tag:list', '/member/tag', '/member/tag/index',
        'lucide:tags', 4, 1);

-- 超级管理员、运营人员均可见会员管理
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 40), (1, 41), (1, 42), (1, 43), (1, 44),
       (2, 40), (2, 41), (2, 42), (2, 43), (2, 44);

-- ------------------------------------------------------------
-- 6. 演示数据：等级、标签、会员、积分流水、打标
-- ------------------------------------------------------------
INSERT IGNORE INTO `mall_member_level` (`id`, `name`, `discount`, `threshold`, `description`, `sort`, `status`)
VALUES (1, '普通会员', 100.0, 0, '注册即享，订单积分 1 元 = 1 分', 1, 1),
       (2, '银卡会员', 98.0, 500, '全场 98 折，生日赠 50 元券包', 2, 1),
       (3, '金卡会员', 95.0, 2000, '全场 95 折，每月赠 20 元无门槛券，专属客服', 3, 1),
       (4, '钻石会员', 90.0, 10000, '全场 9 折，免运费，新品优先购，年度好礼', 4, 1);

INSERT IGNORE INTO `mall_member_tag` (`id`, `name`, `color`, `remark`)
VALUES (1, '高价值用户', 'red', '月消费 ≥ 500 元'),
       (2, '价格敏感', 'orange', '仅在促销期下单'),
       (3, '新客', 'blue', '注册 30 天内'),
       (4, '沉睡用户', 'default', '60 天未访问'),
       (5, '生鲜偏好', 'green', '生鲜类目复购 ≥ 2 次');

-- 演示会员：覆盖三种来源渠道
INSERT IGNORE INTO `app_user` (`id`, `openid`, `nickname`, `avatar`, `phone`, `gender`, `status`,
                               `level_id`, `points`, `source`, `created_at`)
VALUES (1, 'demo-openid-1', '林小满', 'https://picsum.photos/seed/avatar-1/120/120', '13800000001', 1, 1,
        3, 1280, 1, '2026-03-12 09:20:00'),
       (2, 'demo-openid-2', '周予安', 'https://picsum.photos/seed/avatar-2/120/120', '13800000002', 2, 1,
        2, 340, 2, '2026-05-08 14:35:00'),
       (3, 'demo-openid-3', '苏晓棠', 'https://picsum.photos/seed/avatar-3/120/120', '13900000003', 2, 1,
        1, 60, 0, '2026-10-11 20:12:00'),
       (4, 'demo-openid-4', '陆知远', 'https://picsum.photos/seed/avatar-4/120/120', '13700000004', 1, 1,
        3, 2150, 2, '2026-01-26 11:05:00'),
       (5, 'demo-openid-5', '顾清欢', 'https://picsum.photos/seed/avatar-5/120/120', '13600000005', 2, 1,
        2, 890, 2, '2026-06-30 18:44:00'),
       (6, 'demo-openid-6', '沈墨白', 'https://picsum.photos/seed/avatar-6/120/120', '13500000006', 1, 1,
        1, 20, 0, '2026-09-18 08:30:00'),
       (7, 'demo-openid-7', '叶未眠', 'https://picsum.photos/seed/avatar-7/120/120', '13400000007', 2, 1,
        1, 0, 1, '2026-10-20 22:10:00'),
       (8, 'demo-openid-8', '江晚吟', 'https://picsum.photos/seed/avatar-8/120/120', '13300000008', 1, 0,
        1, 120, 2, '2026-04-02 16:25:00');

-- 累计消费与订单数按真实订单聚合回填（重复执行无副作用）
UPDATE `app_user` u
    LEFT JOIN (SELECT user_id,
                      SUM(pay_amount) AS total_spend,
                      COUNT(*)        AS order_count
               FROM mall_order
               WHERE status IN (1, 2, 3)
                 AND deleted = 0
               GROUP BY user_id) o ON o.user_id = u.id
SET u.total_spend = IFNULL(o.total_spend, 0),
    u.order_count = IFNULL(o.order_count, 0);

INSERT IGNORE INTO `mall_points_log` (`id`, `user_id`, `change`, `balance`, `type`, `order_no`, `remark`, `created_at`)
VALUES (5001, 1, 120, 120, 'earn', 'EM20261018000005', '订单消费获得', '2026-10-21 18:20:00'),
       (5002, 1, 119, 239, 'earn', 'EM20261024000007', '订单消费获得', '2026-10-24 18:00:00'),
       (5003, 1, -78, 1280, 'consume', NULL, '积分兑换 10 元优惠券', '2026-10-25 10:00:00'),
       (5004, 2, 68, 68, 'earn', 'EM20261023000004', '订单消费获得', '2026-10-23 16:40:00'),
       (5005, 2, 300, 340, 'adjust', NULL, '双十一活动赠送', '2026-10-24 00:00:00'),
       (5006, 4, 2150, 2150, 'earn', 'EM20261015000008', '订单消费获得', '2026-10-17 10:05:00'),
       (5007, 5, 890, 890, 'earn', NULL, '历史积分迁移', '2026-10-01 09:00:00');

INSERT IGNORE INTO `mall_member_user_tag` (`id`, `user_id`, `tag_id`)
VALUES (6001, 1, 1), (6002, 1, 5), (6003, 2, 2), (6004, 4, 1),
       (6005, 5, 2), (6006, 3, 3), (6007, 7, 3), (6008, 6, 4), (6009, 8, 4);
