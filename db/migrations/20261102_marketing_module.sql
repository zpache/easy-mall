-- ============================================================
-- 营销中心模块：优惠券、拼团秒杀、签到、满减、菜单与授权、演示数据
-- 幂等：可重复执行
-- ============================================================

-- ------------------------------------------------------------
-- 1. 优惠券
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mk_coupon`
(
    `id`          bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(100)  NOT NULL COMMENT '券名称',
    `type`        tinyint       NOT NULL DEFAULT 1 COMMENT '1 满减券 2 折扣券 3 无门槛券',
    `amount`      decimal(10, 2)         DEFAULT NULL COMMENT '面额（元）',
    `discount`    decimal(4, 1)          DEFAULT NULL COMMENT '折扣（百分比，90 表示 9 折）',
    `threshold`   decimal(10, 2) NOT NULL DEFAULT 0 COMMENT '使用门槛（元），0 表示无门槛',
    `total_count` int           NOT NULL DEFAULT 0 COMMENT '发放总量',
    `send_count`  int           NOT NULL DEFAULT 0 COMMENT '已领取数量',
    `used_count`  int           NOT NULL DEFAULT 0 COMMENT '已使用数量',
    `per_limit`   int           NOT NULL DEFAULT 1 COMMENT '每人限领张数',
    `valid_type`  tinyint       NOT NULL DEFAULT 1 COMMENT '1 固定日期 2 领取后 N 天',
    `valid_start` date                   DEFAULT NULL COMMENT '有效期开始',
    `valid_end`   date                   DEFAULT NULL COMMENT '有效期结束',
    `valid_days`  int                    DEFAULT NULL COMMENT '领取后有效天数',
    `status`      tinyint       NOT NULL DEFAULT 1 COMMENT '1 启用，0 停用',
    `remark`      varchar(255)           DEFAULT NULL COMMENT '备注',
    `created_at`  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`  bigint                 DEFAULT NULL,
    `updated_by`  bigint                 DEFAULT NULL,
    `version`     int           NOT NULL DEFAULT 0,
    `deleted`     tinyint       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_coupon_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='优惠券';

-- ------------------------------------------------------------
-- 2. 领券记录
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mk_coupon_user`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `coupon_id`   bigint      NOT NULL COMMENT '券 ID',
    `user_id`     bigint      NOT NULL COMMENT '会员 ID',
    `status`      tinyint     NOT NULL DEFAULT 0 COMMENT '0 未使用 1 已使用 2 已过期',
    `order_no`    varchar(32)          DEFAULT NULL COMMENT '核销订单号',
    `received_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    `used_at`     datetime             DEFAULT NULL COMMENT '使用时间',
    `expired_at`  datetime             DEFAULT NULL COMMENT '过期时间',
    PRIMARY KEY (`id`),
    KEY `idx_cu_coupon` (`coupon_id`, `status`),
    KEY `idx_cu_user` (`user_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='领券记录';

-- ------------------------------------------------------------
-- 3. 拼团秒杀活动
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mk_promotion`
(
    `id`             bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           varchar(100)   NOT NULL COMMENT '活动名称',
    `type`           tinyint        NOT NULL DEFAULT 1 COMMENT '1 秒杀 2 拼团',
    `goods_id`       bigint         NOT NULL COMMENT '商品 ID',
    `goods_name`     varchar(200)   NOT NULL COMMENT '商品名称快照',
    `original_price` decimal(10, 2) NOT NULL COMMENT '原价（元）',
    `promo_price`    decimal(10, 2) NOT NULL COMMENT '活动价（秒杀价 / 拼团价）',
    `group_size`     int                     DEFAULT NULL COMMENT '成团人数（拼团必填）',
    `group_hours`    int                     DEFAULT NULL COMMENT '成团有效时长（小时，拼团）',
    `stock`          int            NOT NULL DEFAULT 0 COMMENT '活动库存',
    `sold_count`     int            NOT NULL DEFAULT 0 COMMENT '已售数量',
    `start_time`     datetime       NOT NULL COMMENT '开始时间',
    `end_time`       datetime       NOT NULL COMMENT '结束时间',
    `status`         tinyint        NOT NULL DEFAULT 1 COMMENT '1 启用，0 停用',
    `created_at`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`     bigint                  DEFAULT NULL,
    `updated_by`     bigint                  DEFAULT NULL,
    `version`        int            NOT NULL DEFAULT 0,
    `deleted`        tinyint        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_promo_type` (`type`, `status`),
    KEY `idx_promo_goods` (`goods_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='拼团秒杀活动';

-- ------------------------------------------------------------
-- 4. 签到规则与签到记录
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mk_signin_rule`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`             varchar(100) NOT NULL COMMENT '规则名称',
    `points_per_day`   int          NOT NULL DEFAULT 1 COMMENT '每日签到积分',
    `bonus_every_days` int          NOT NULL DEFAULT 0 COMMENT '每连续签到 N 天额外奖励，0 表示不启用',
    `bonus_points`     int          NOT NULL DEFAULT 0 COMMENT '连续签到额外奖励积分',
    `status`           tinyint      NOT NULL DEFAULT 1 COMMENT '1 启用，0 停用',
    `remark`           varchar(255)          DEFAULT NULL COMMENT '备注',
    `created_at`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`       bigint                DEFAULT NULL,
    `updated_by`       bigint                DEFAULT NULL,
    `version`          int          NOT NULL DEFAULT 0,
    `deleted`          tinyint      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='签到规则';

CREATE TABLE IF NOT EXISTS `mk_signin_log`
(
    `id`              bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         bigint   NOT NULL COMMENT '会员 ID',
    `sign_date`       date     NOT NULL COMMENT '签到日期',
    `continuous_days` int      NOT NULL DEFAULT 1 COMMENT '连续签到天数',
    `points`          int      NOT NULL DEFAULT 0 COMMENT '本次获得积分',
    `created_at`      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_signin_user_date` (`user_id`, `sign_date`),
    KEY `idx_signin_date` (`sign_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='会员签到记录';

-- ------------------------------------------------------------
-- 5. 满减活动
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mk_reduction`
(
    `id`         bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       varchar(100)   NOT NULL COMMENT '活动名称',
    `mode`       tinyint        NOT NULL DEFAULT 1 COMMENT '1 阶梯满减 2 每满减',
    `threshold`  decimal(10, 2) NOT NULL COMMENT '满 X 元',
    `benefit`    decimal(10, 2) NOT NULL COMMENT '减 Y 元',
    `start_time` datetime       NOT NULL COMMENT '开始时间',
    `end_time`   datetime       NOT NULL COMMENT '结束时间',
    `status`     tinyint        NOT NULL DEFAULT 1 COMMENT '1 启用，0 停用',
    `created_at` datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` bigint                  DEFAULT NULL,
    `updated_by` bigint                  DEFAULT NULL,
    `version`    int            NOT NULL DEFAULT 0,
    `deleted`    tinyint        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='满减活动';

-- ------------------------------------------------------------
-- 6. 菜单：营销中心（目录 + 4 个子菜单）
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `pid`, `name`, `title`, `type`, `auth_code`, `path`, `component`, `icon`,
                                `order_no`, `status`)
VALUES (50, 0, 'Marketing', '营销中心', 'catalog', NULL, '/marketing', NULL, 'lucide:megaphone', 30, 1),
       (51, 50, 'MarketingCoupon', '优惠券', 'menu', 'marketing:coupon:list', '/marketing/coupon',
        '/marketing/coupon/index', 'lucide:ticket-percent', 1, 1),
       (52, 50, 'MarketingPromotion', '拼团秒杀', 'menu', 'marketing:promotion:list', '/marketing/promotion',
        '/marketing/promotion/index', 'lucide:zap', 2, 1),
       (53, 50, 'MarketingSignin', '签到管理', 'menu', 'marketing:signin:list', '/marketing/signin',
        '/marketing/signin/index', 'lucide:calendar-check', 3, 1),
       (54, 50, 'MarketingReduction', '满减活动', 'menu', 'marketing:reduction:list', '/marketing/reduction',
        '/marketing/reduction/index', 'lucide:badge-percent', 4, 1);

-- 超级管理员、运营人员均可见营销中心
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 50), (1, 51), (1, 52), (1, 53), (1, 54),
       (2, 50), (2, 51), (2, 52), (2, 53), (2, 54);

-- ------------------------------------------------------------
-- 7. 演示数据
-- ------------------------------------------------------------
INSERT IGNORE INTO `mk_coupon` (`id`, `name`, `type`, `amount`, `discount`, `threshold`, `total_count`, `send_count`,
                                `used_count`, `per_limit`, `valid_type`, `valid_start`, `valid_end`, `status`, `remark`)
VALUES (1, '新人专享 10 元券', 3, 10.00, NULL, 0.00, 1000, 356, 121, 1, 2, NULL, NULL, 1, '注册后 7 天内可领'),
       (2, '全场满 99 减 15', 1, 15.00, NULL, 99.00, 5000, 1284, 396, 2, 1, '2026-10-01', '2026-11-11', 1, '双十一预热'),
       (3, '生鲜品类 9 折券', 2, NULL, 90.0, 50.00, 2000, 487, 132, 1, 1, '2026-10-15', '2026-10-31', 1, '生鲜水果可用'),
       (4, '满 199 减 30 大额券', 1, 30.00, NULL, 199.00, 1000, 233, 65, 1, 1, '2026-11-01', '2026-11-11', 0, '双十一当天开抢'),
       (5, '会员日 5 元券', 3, 5.00, NULL, 0.00, 8000, 3210, 890, 1, 1, '2026-10-01', '2026-12-31', 1, '每月 8 日会员日发放');

INSERT IGNORE INTO `mk_coupon_user` (`id`, `coupon_id`, `user_id`, `status`, `order_no`, `received_at`, `used_at`)
VALUES (7001, 2, 1, 1, 'EM20261018000005', '2026-10-17 10:02:00', '2026-10-18 18:20:00'),
       (7002, 5, 1, 0, NULL, '2026-10-25 09:30:00', NULL),
       (7003, 2, 2, 0, NULL, '2026-10-23 16:40:00', NULL),
       (7004, 3, 4, 1, 'EM20261015000008', '2026-10-15 08:12:00', '2026-10-17 10:05:00'),
       (7005, 1, 3, 0, NULL, '2026-10-11 20:15:00', NULL),
       (7006, 5, 5, 2, NULL, '2026-10-02 12:00:00', NULL),
       (7007, 2, 5, 1, 'EM20261023000004', '2026-10-22 19:20:00', '2026-10-23 16:40:00');

INSERT IGNORE INTO `mk_promotion` (`id`, `name`, `type`, `goods_id`, `goods_name`, `original_price`, `promo_price`,
                                   `group_size`, `group_hours`, `stock`, `sold_count`, `start_time`, `end_time`, `status`)
VALUES (1, '脐橙季限时秒杀', 1, 1, '当季赣南脐橙 5 斤装', 29.90, 19.90, NULL, NULL, 500, 213, '2026-10-25 10:00:00',
        '2026-10-31 22:00:00', 1),
       (2, '坚果 3 人拼团', 2, 3, '每日坚果混合装 30 包', 59.90, 45.90, 3, 24, 300, 86, '2026-10-24 00:00:00',
        '2026-11-05 23:59:59', 1),
       (3, '周五零点秒杀场', 1, 2, '智利车厘子 JJ 级 2 斤', 89.00, 69.00, NULL, NULL, 200, 47, '2026-10-31 00:00:00',
        '2026-10-31 08:00:00', 1),
       (4, '牛乳 2 人拼团', 2, 4, '低温鲜牛乳 950ml x 6', 69.00, 55.20, 2, 12, 150, 12, '2026-10-26 08:00:00',
        '2026-11-02 22:00:00', 1),
       (5, '国庆坚果秒杀', 1, 3, '每日坚果混合装 30 包', 59.90, 39.90, NULL, NULL, 100, 100, '2026-10-01 00:00:00',
        '2026-10-07 23:59:59', 0),
       (6, '纸巾 3 人拼团', 2, 5, '原木抽纸 3 层 100 抽 x 24 包', 49.90, 36.90, 3, 24, 400, 158, '2026-10-20 00:00:00',
        '2026-10-27 23:59:59', 1);

INSERT IGNORE INTO `mk_signin_rule` (`id`, `name`, `points_per_day`, `bonus_every_days`, `bonus_points`, `status`, `remark`)
VALUES (1, '基础签到', 1, 7, 10, 1, '连续签到满 7 天额外奖励 10 积分'),
       (2, '双十一定向翻倍', 2, 11, 50, 0, '活动期间每日签到双倍，连续 11 天再奖 50 积分');

INSERT IGNORE INTO `mk_signin_log` (`id`, `user_id`, `sign_date`, `continuous_days`, `points`, `created_at`)
VALUES (8001, 1, '2026-10-23', 5, 1, '2026-10-23 08:10:00'),
       (8002, 1, '2026-10-24', 6, 1, '2026-10-24 07:55:00'),
       (8003, 1, '2026-10-25', 7, 11, '2026-10-25 08:30:00'),
       (8004, 1, '2026-10-26', 1, 1, '2026-10-26 09:05:00'),
       (8005, 2, '2026-10-25', 2, 1, '2026-10-25 21:40:00'),
       (8006, 2, '2026-10-26', 3, 1, '2026-10-26 20:15:00'),
       (8007, 4, '2026-10-26', 1, 1, '2026-10-26 06:30:00'),
       (8008, 5, '2026-10-24', 4, 1, '2026-10-24 12:20:00'),
       (8009, 5, '2026-10-25', 5, 1, '2026-10-25 11:50:00'),
       (8010, 5, '2026-10-26', 6, 1, '2026-10-26 10:40:00');

INSERT IGNORE INTO `mk_reduction` (`id`, `name`, `mode`, `threshold`, `benefit`, `start_time`, `end_time`, `status`)
VALUES (1, '满 99 减 10', 2, 99.00, 10.00, '2026-10-01 00:00:00', '2026-10-31 23:59:59', 1),
       (2, '阶梯满减：199-20 / 399-50', 1, 199.00, 20.00, '2026-11-01 00:00:00', '2026-11-11 23:59:59', 1),
       (3, '阶梯满减：599-80', 1, 599.00, 80.00, '2026-11-01 00:00:00', '2026-11-11 23:59:59', 1),
       (4, '国庆满 59 减 5', 2, 59.00, 5.00, '2026-10-01 00:00:00', '2026-10-07 23:59:59', 0);
