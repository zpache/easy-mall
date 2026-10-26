-- ============================================================
-- easy-mall 数据库初始化脚本（MySQL 8.0+）
-- 用法：mysql -uroot -p < db/easy_mall.sql
-- 说明：admin 与 api 两个服务共用同一个库，只是访问的表不同。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `easy_mall`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `easy_mall`;

-- ------------------------------------------------------------
-- 管理后台账号
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`      varchar(32)  NOT NULL COMMENT '登录账号',
    `password`      varchar(100) NOT NULL COMMENT 'BCrypt 密文',
    `nickname`      varchar(32)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar`        varchar(255)          DEFAULT NULL COMMENT '头像',
    `phone`         varchar(20)           DEFAULT NULL COMMENT '手机号',
    `role`          varchar(20)  NOT NULL DEFAULT 'OPERATOR' COMMENT '角色：ADMIN/OPERATOR',
    `status`        tinyint      NOT NULL DEFAULT 1 COMMENT '1 正常，0 禁用',
    `last_login_at` datetime              DEFAULT NULL COMMENT '最后登录时间',
    `created_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`    bigint       DEFAULT NULL COMMENT '创建人 ID',
    `updated_by`    bigint       DEFAULT NULL COMMENT '最后修改人 ID',
    `version`       int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`       tinyint      NOT NULL DEFAULT 0 COMMENT '0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='管理后台账号';

-- ------------------------------------------------------------
-- 商品分类
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `mall_category`;
CREATE TABLE `mall_category`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT,
    `name`       varchar(32) NOT NULL COMMENT '分类名称',
    `parent_id`  bigint      NOT NULL DEFAULT 0 COMMENT '上级分类，0 为一级分类',
    `icon`       varchar(255)         DEFAULT NULL COMMENT '分类图标',
    `sort`       int         NOT NULL DEFAULT 0 COMMENT '排序值，越小越前',
    `status`     tinyint     NOT NULL DEFAULT 1 COMMENT '1 启用，0 停用',
    `created_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` bigint       DEFAULT NULL COMMENT '创建人 ID',
    `updated_by` bigint       DEFAULT NULL COMMENT '最后修改人 ID',
    `version`    int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`    tinyint     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_category_parent` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品分类';

-- ------------------------------------------------------------
-- 商品（单规格）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `mall_goods`;
CREATE TABLE `mall_goods`
(
    `id`             bigint         NOT NULL AUTO_INCREMENT,
    `category_id`    bigint         NOT NULL COMMENT '分类 ID',
    `name`           varchar(100)   NOT NULL COMMENT '商品名称',
    `subtitle`       varchar(200)            DEFAULT NULL COMMENT '副标题',
    `main_image`     varchar(255)   NOT NULL COMMENT '主图',
    `images`         varchar(2000)           DEFAULT NULL COMMENT '轮播图，英文逗号分隔',
    `price`          decimal(10, 2) NOT NULL COMMENT '销售价',
    `original_price` decimal(10, 2)          DEFAULT NULL COMMENT '划线价',
    `stock`          int            NOT NULL DEFAULT 0 COMMENT '库存',
    `sales`          int            NOT NULL DEFAULT 0 COMMENT '销量',
    `unit`           varchar(16)             DEFAULT '件' COMMENT '单位',
    `detail`         mediumtext              DEFAULT NULL COMMENT '详情富文本',
    `status`         tinyint        NOT NULL DEFAULT 1 COMMENT '1 上架，0 下架',
    `sort`           int            NOT NULL DEFAULT 0,
    `created_at`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`     bigint       DEFAULT NULL COMMENT '创建人 ID',
    `updated_by`     bigint       DEFAULT NULL COMMENT '最后修改人 ID',
    `version`        int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`        tinyint        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_goods_category` (`category_id`),
    KEY `idx_goods_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品';

-- ------------------------------------------------------------
-- 小程序用户
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `app_user`;
CREATE TABLE `app_user`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT,
    `openid`     varchar(64) NOT NULL COMMENT '微信 openid',
    `unionid`    varchar(64)          DEFAULT NULL COMMENT '微信 unionid',
    `nickname`   varchar(64)          DEFAULT NULL COMMENT '昵称',
    `avatar`     varchar(255)         DEFAULT NULL COMMENT '头像',
    `phone`      varchar(20)          DEFAULT NULL COMMENT '手机号',
    `gender`     tinyint     NOT NULL DEFAULT 0 COMMENT '0 未知，1 男，2 女',
    `status`     tinyint     NOT NULL DEFAULT 1 COMMENT '1 正常，0 禁用',
    `created_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` bigint       DEFAULT NULL COMMENT '创建人 ID',
    `updated_by` bigint       DEFAULT NULL COMMENT '最后修改人 ID',
    `version`    int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`    tinyint     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_user_openid` (`openid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='小程序用户';

-- ------------------------------------------------------------
-- 收货地址
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT,
    `user_id`    bigint      NOT NULL,
    `receiver`   varchar(32) NOT NULL COMMENT '收货人',
    `phone`      varchar(20) NOT NULL COMMENT '联系电话',
    `province`   varchar(32) NOT NULL,
    `city`       varchar(32) NOT NULL,
    `district`   varchar(32) NOT NULL,
    `detail`     varchar(255) NOT NULL COMMENT '详细地址',
    `is_default` tinyint     NOT NULL DEFAULT 0 COMMENT '1 默认地址',
    `created_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` bigint       DEFAULT NULL COMMENT '创建人 ID',
    `updated_by` bigint       DEFAULT NULL COMMENT '最后修改人 ID',
    `version`    int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`    tinyint     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_address_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='收货地址';

-- ------------------------------------------------------------
-- 购物车
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item`
(
    `id`         bigint   NOT NULL AUTO_INCREMENT,
    `user_id`    bigint   NOT NULL,
    `goods_id`   bigint   NOT NULL,
    `quantity`   int      NOT NULL DEFAULT 1 COMMENT '数量',
    `checked`    tinyint  NOT NULL DEFAULT 1 COMMENT '1 选中',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    tinyint  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cart_user_goods` (`user_id`, `goods_id`, `deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='购物车';

-- ------------------------------------------------------------
-- 订单
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `mall_order`;
CREATE TABLE `mall_order`
(
    `id`               bigint         NOT NULL AUTO_INCREMENT,
    `order_no`         varchar(32)    NOT NULL COMMENT '订单号',
    `user_id`          bigint         NOT NULL,
    `total_amount`     decimal(10, 2) NOT NULL COMMENT '商品总额',
    `freight_amount`   decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '运费',
    `pay_amount`       decimal(10, 2) NOT NULL COMMENT '应付金额',
    `status`           tinyint        NOT NULL DEFAULT 0 COMMENT '0 待付款 1 待发货 2 待收货 3 已完成 4 已取消',
    `receiver_name`    varchar(32)    NOT NULL,
    `receiver_phone`   varchar(20)    NOT NULL,
    `receiver_address` varchar(255)   NOT NULL,
    `remark`           varchar(255)            DEFAULT NULL,
    `pay_time`         datetime                DEFAULT NULL,
    `delivery_time`    datetime                DEFAULT NULL,
    `finish_time`      datetime                DEFAULT NULL,
    `cancel_time`      datetime                DEFAULT NULL,
    `created_at`       datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`       bigint       DEFAULT NULL COMMENT '创建人 ID',
    `updated_by`       bigint       DEFAULT NULL COMMENT '最后修改人 ID',
    `version`          int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`          tinyint        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_order_user` (`user_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单';

DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item`
(
    `id`           bigint         NOT NULL AUTO_INCREMENT,
    `order_id`     bigint         NOT NULL,
    `goods_id`     bigint         NOT NULL,
    `goods_name`   varchar(100)   NOT NULL,
    `goods_image`  varchar(255)   NOT NULL,
    `price`        decimal(10, 2) NOT NULL COMMENT '下单时单价',
    `quantity`     int            NOT NULL,
    `total_amount` decimal(10, 2) NOT NULL,
    `created_at`   datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`   bigint       DEFAULT NULL COMMENT '创建人 ID',
    `updated_by`   bigint       DEFAULT NULL COMMENT '最后修改人 ID',
    `version`      int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`      tinyint        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order_item_order` (`order_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单明细';

-- ------------------------------------------------------------
-- 初始化数据
-- 默认管理员：admin / admin123
-- ------------------------------------------------------------
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role`, `status`)
VALUES ('admin', '$2a$10$p4026qPjmgn80u.iSAh7BOwesH4y6Vc2SsT8eTwpPOLbS0Sg9RWKi', '超级管理员', 'ADMIN', 1);

INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `sort`, `status`)
VALUES (1, '新鲜水果', 0, 1, 1),
       (2, '休闲零食', 0, 2, 1),
       (3, '茶饮冲调', 0, 3, 1);

INSERT INTO `mall_goods` (`category_id`, `name`, `subtitle`, `main_image`, `images`, `price`, `original_price`, `stock`,
                          `sales`, `unit`, `detail`, `status`, `sort`)
VALUES (1, '当季赣南脐橙 5 斤装', '现摘现发，酸甜多汁', 'https://picsum.photos/seed/orange/600/600',
        'https://picsum.photos/seed/orange/600/600,https://picsum.photos/seed/orange2/600/600', 29.90, 39.90, 200, 12,
        '箱', '<p>产地直发，果径 70-80mm。</p>', 1, 1),
       (1, '海南贵妃芒 3 斤装', '皮薄核小，香甜细腻', 'https://picsum.photos/seed/mango/600/600',
        'https://picsum.photos/seed/mango/600/600', 35.00, 45.00, 150, 8, '箱',
        '<p>自然成熟，冷链发货。</p>', 1, 2),
       (2, '每日坚果混合装 30 包', '6 种坚果，独立小包', 'https://picsum.photos/seed/nuts/600/600',
        'https://picsum.photos/seed/nuts/600/600', 59.90, 79.00, 300, 45, '袋',
        '<p>每日一包，营养均衡。</p>', 1, 3),
       (2, '手工蛋卷 500g', '酥脆掉渣，蛋香浓郁', 'https://picsum.photos/seed/cookie/600/600',
        'https://picsum.photos/seed/cookie/600/600', 19.90, 25.90, 500, 60, '盒',
        '<p>传统工艺，新鲜现烤。</p>', 1, 4),
       (3, '云南小粒咖啡豆 250g', '中深烘焙，坚果回甘', 'https://picsum.photos/seed/coffee/600/600',
        'https://picsum.photos/seed/coffee/600/600', 68.00, 88.00, 80, 20, '袋',
        '<p>保山产区，现磨现发。</p>', 1, 5),
       (3, '冷萃乌龙茶包 20 袋', '三秒冷泡，清香回甘', 'https://picsum.photos/seed/tea/600/600',
        'https://picsum.photos/seed/tea/600/600', 22.90, 28.90, 400, 33, '盒',
        '<p>冷热双泡，随时来一杯。</p>', 1, 6);
