-- ============================================================
-- 订单管理模块：订单发货字段、售后退款表、商品评价表、菜单与授权
-- 幂等：可重复执行
-- ============================================================

-- ------------------------------------------------------------
-- 1. mall_order 增补发货与退款字段（MySQL 无 ADD COLUMN IF NOT EXISTS，用动态 SQL 保证幂等）
-- ------------------------------------------------------------
SET @has_delivery_company := (SELECT COUNT(*)
                              FROM information_schema.COLUMNS
                              WHERE TABLE_SCHEMA = DATABASE()
                                AND TABLE_NAME = 'mall_order'
                                AND COLUMN_NAME = 'delivery_company');
SET @sql := IF(@has_delivery_company = 0,
               'ALTER TABLE `mall_order` ADD COLUMN `delivery_company` varchar(50) DEFAULT NULL COMMENT ''快递公司'' AFTER `receiver_address`',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_delivery_no := (SELECT COUNT(*)
                         FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE()
                           AND TABLE_NAME = 'mall_order'
                           AND COLUMN_NAME = 'delivery_no');
SET @sql := IF(@has_delivery_no = 0,
               'ALTER TABLE `mall_order` ADD COLUMN `delivery_no` varchar(64) DEFAULT NULL COMMENT ''快递单号'' AFTER `delivery_company`',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_refund_status := (SELECT COUNT(*)
                           FROM information_schema.COLUMNS
                           WHERE TABLE_SCHEMA = DATABASE()
                             AND TABLE_NAME = 'mall_order'
                             AND COLUMN_NAME = 'refund_status');
SET @sql := IF(@has_refund_status = 0,
               'ALTER TABLE `mall_order` ADD COLUMN `refund_status` tinyint NOT NULL DEFAULT 0 COMMENT ''0 无售后 1 退款中 2 已退款 3 售后被拒'' AFTER `status`',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_cancel_reason := (SELECT COUNT(*)
                           FROM information_schema.COLUMNS
                           WHERE TABLE_SCHEMA = DATABASE()
                             AND TABLE_NAME = 'mall_order'
                             AND COLUMN_NAME = 'cancel_reason');
SET @sql := IF(@has_cancel_reason = 0,
               'ALTER TABLE `mall_order` ADD COLUMN `cancel_reason` varchar(255) DEFAULT NULL COMMENT ''取消原因'' AFTER `cancel_time`',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2. 售后退款
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mall_order_refund`
(
    `id`           bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `refund_no`    varchar(32)    NOT NULL COMMENT '退款单号',
    `order_id`     bigint         NOT NULL COMMENT '订单 ID',
    `order_no`     varchar(32)    NOT NULL COMMENT '订单号（冗余，便于列表展示）',
    `user_id`      bigint         NOT NULL COMMENT '下单用户 ID',
    `amount`       decimal(10, 2) NOT NULL COMMENT '退款金额',
    `type`         varchar(16)    NOT NULL DEFAULT 'refund' COMMENT 'refund 仅退款 / return 退货退款',
    `reason`       varchar(255)            DEFAULT NULL COMMENT '买家申请原因',
    `status`       tinyint        NOT NULL DEFAULT 0 COMMENT '0 待审核 1 待退款 2 已拒绝 3 已退款',
    `audit_remark` varchar(255)            DEFAULT NULL COMMENT '商家审核意见',
    `applied_at`   datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `handled_at`   datetime                DEFAULT NULL COMMENT '处理时间',
    `handled_by`   bigint                  DEFAULT NULL COMMENT '处理人 ID',
    `created_at`   datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`   bigint                  DEFAULT NULL COMMENT '创建人 ID',
    `updated_by`   bigint                  DEFAULT NULL COMMENT '最后修改人 ID',
    `version`      int            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`      tinyint        NOT NULL DEFAULT 0 COMMENT '0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_no` (`refund_no`),
    KEY `idx_refund_order` (`order_id`),
    KEY `idx_refund_status` (`status`, `applied_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单售后退款';

-- ------------------------------------------------------------
-- 3. 商品评价
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mall_order_comment`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`      bigint       NOT NULL COMMENT '订单 ID',
    `order_no`      varchar(32)  NOT NULL COMMENT '订单号',
    `goods_id`      bigint       NOT NULL COMMENT '商品 ID',
    `goods_name`    varchar(100) NOT NULL COMMENT '商品名称快照',
    `goods_image`   varchar(255)          DEFAULT NULL COMMENT '商品图片快照',
    `user_id`       bigint       NOT NULL COMMENT '评价用户 ID',
    `user_nickname` varchar(64)           DEFAULT NULL COMMENT '评价用户昵称快照',
    `score`         tinyint      NOT NULL DEFAULT 5 COMMENT '评分 1-5',
    `content`       varchar(500)          DEFAULT NULL COMMENT '评价内容',
    `images`        varchar(1000)         DEFAULT NULL COMMENT '晒图，多张用英文逗号分隔',
    `reply`         varchar(500)          DEFAULT NULL COMMENT '商家回复',
    `replied_at`    datetime              DEFAULT NULL COMMENT '回复时间',
    `is_show`       tinyint      NOT NULL DEFAULT 1 COMMENT '1 显示，0 隐藏',
    `commented_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    `created_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`    bigint                DEFAULT NULL COMMENT '创建人 ID',
    `updated_by`    bigint                DEFAULT NULL COMMENT '最后修改人 ID',
    `version`       int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`       tinyint      NOT NULL DEFAULT 0 COMMENT '0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_comment_order` (`order_id`),
    KEY `idx_comment_goods` (`goods_id`),
    KEY `idx_comment_show` (`is_show`, `commented_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单商品评价';

-- ------------------------------------------------------------
-- 4. 菜单：订单管理（目录 + 4 个子菜单）
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `pid`, `name`, `title`, `type`, `auth_code`, `path`, `component`, `icon`,
                                `order_no`, `status`)
VALUES (30, 0, 'Order', '订单管理', 'catalog', NULL, '/order', NULL, 'lucide:receipt-text', 15, 1),
       (31, 30, 'OrderList', '订单列表', 'menu', 'order:order:list', '/order/list', '/order/list/index',
        'lucide:clipboard-list', 1, 1),
       (32, 30, 'OrderDelivery', '待发货', 'menu', 'order:order:deliver', '/order/delivery', '/order/delivery/index',
        'lucide:truck', 2, 1),
       (33, 30, 'OrderRefund', '售后退款', 'menu', 'order:refund:list', '/order/refund', '/order/refund/index',
        'lucide:rotate-ccw', 3, 1),
       (34, 30, 'OrderComment', '评价管理', 'menu', 'order:comment:list', '/order/comment', '/order/comment/index',
        'lucide:message-square-quote', 4, 1);

-- 超级管理员、运营人员均可见订单管理
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 30), (1, 31), (1, 32), (1, 33), (1, 34),
       (2, 30), (2, 31), (2, 32), (2, 33), (2, 34);

-- ------------------------------------------------------------
-- 5. 演示数据：多状态订单、订单明细、售后单、评价
-- 固定主键 + INSERT IGNORE，重复执行不会产生重复数据
-- ------------------------------------------------------------
INSERT IGNORE INTO `app_user` (`id`, `openid`, `nickname`, `avatar`, `status`)
VALUES (1, 'demo-openid-1', '微信用户', 'https://picsum.photos/seed/avatar-1/120/120', 1),
       (2, 'demo-openid-2', '山野小食客', 'https://picsum.photos/seed/avatar-2/120/120', 1);

INSERT IGNORE INTO `mall_order` (`id`, `order_no`, `user_id`, `total_amount`, `freight_amount`, `pay_amount`, `status`,
                                 `refund_status`, `receiver_name`, `receiver_phone`, `receiver_address`,
                                 `delivery_company`, `delivery_no`, `remark`,
                                 `pay_time`, `delivery_time`, `finish_time`, `cancel_time`, `cancel_reason`, `created_at`)
VALUES (1001, 'EM20261020000001', 1, 29.90, 0.00, 29.90, 0, 0, '林小满', '13800000001', '上海市浦东新区张江路 88 号 3 号楼 501',
        NULL, NULL, '尽快发货', NULL, NULL, NULL, NULL, NULL, '2026-10-20 10:12:00'),
       (1002, 'EM20261021000002', 2, 119.70, 0.00, 119.70, 1, 1, '周予安', '13800000002', '杭州市西湖区文三路 199 号 A 座 1201',
        NULL, NULL, '放门口即可', '2026-10-21 09:30:00', NULL, NULL, NULL, NULL, '2026-10-21 09:28:00'),
       (1003, 'EM20261022000003', 1, 35.00, 8.00, 43.00, 1, 0, '林小满', '13800000001', '上海市浦东新区张江路 88 号 3 号楼 501',
        NULL, NULL, NULL, '2026-10-22 20:05:00', NULL, NULL, NULL, NULL, '2026-10-22 20:03:00'),
       (1004, 'EM20261023000004', 2, 68.00, 0.00, 68.00, 2, 0, '周予安', '13800000002', '杭州市西湖区文三路 199 号 A 座 1201',
        '顺丰速运', 'SF7890123456789', NULL, '2026-10-23 11:20:00', '2026-10-23 16:40:00', NULL, NULL, NULL,
        '2026-10-23 11:18:00'),
       (1005, 'EM20261018000005', 1, 59.70, 0.00, 59.70, 3, 3, '林小满', '13800000001', '上海市浦东新区张江路 88 号 3 号楼 501',
        '中通快递', 'ZT1122334455667', NULL, '2026-10-18 15:02:00', '2026-10-19 09:10:00', '2026-10-21 18:20:00', NULL, NULL,
        '2026-10-18 15:00:00'),
       (1006, 'EM20261019000006', 2, 22.90, 0.00, 22.90, 4, 0, '周予安', '13800000002', '杭州市西湖区文三路 199 号 A 座 1201',
        NULL, NULL, NULL, NULL, NULL, NULL, '2026-10-19 21:05:00', '超时未支付，系统自动关闭', '2026-10-19 20:35:00'),
       (1007, 'EM20261024000007', 1, 119.80, 0.00, 119.80, 2, 1, '林小满', '13800000001', '上海市浦东新区张江路 88 号 3 号楼 501',
        '圆通速递', 'YT9988776655443', NULL, '2026-10-24 13:15:00', '2026-10-24 18:00:00', NULL, NULL, NULL,
        '2026-10-24 13:12:00'),
       (1008, 'EM20261015000008', 2, 35.00, 0.00, 35.00, 3, 2, '周予安', '13800000002', '杭州市西湖区文三路 199 号 A 座 1201',
        '顺丰速运', 'SF1234509876543', NULL, '2026-10-15 08:40:00', '2026-10-15 14:20:00', '2026-10-17 10:05:00', NULL, NULL,
        '2026-10-15 08:38:00');

INSERT IGNORE INTO `order_item` (`id`, `order_id`, `goods_id`, `goods_name`, `goods_image`, `price`, `quantity`,
                                 `total_amount`, `created_at`)
VALUES (2001, 1001, 1, '当季赣南脐橙 5 斤装', 'https://picsum.photos/seed/orange/600/600', 29.90, 1, 29.90, '2026-10-20 10:12:00'),
       (2002, 1002, 1, '当季赣南脐橙 5 斤装', 'https://picsum.photos/seed/orange/600/600', 29.90, 2, 59.80, '2026-10-21 09:28:00'),
       (2003, 1002, 3, '每日坚果混合装 30 包', 'https://picsum.photos/seed/nuts/600/600', 59.90, 1, 59.90, '2026-10-21 09:28:00'),
       (2004, 1003, 2, '海南贵妃芒 3 斤装', 'https://picsum.photos/seed/mango/600/600', 35.00, 1, 35.00, '2026-10-22 20:03:00'),
       (2005, 1004, 5, '云南小粒咖啡豆 250g', 'https://picsum.photos/seed/coffee/600/600', 68.00, 1, 68.00, '2026-10-23 11:18:00'),
       (2006, 1005, 4, '手工蛋卷 500g', 'https://picsum.photos/seed/cookie/600/600', 19.90, 3, 59.70, '2026-10-18 15:00:00'),
       (2007, 1006, 6, '冷萃乌龙茶包 20 袋', 'https://picsum.photos/seed/tea/600/600', 22.90, 1, 22.90, '2026-10-19 20:35:00'),
       (2008, 1007, 3, '每日坚果混合装 30 包', 'https://picsum.photos/seed/nuts/600/600', 59.90, 2, 119.80, '2026-10-24 13:12:00'),
       (2009, 1008, 2, '海南贵妃芒 3 斤装', 'https://picsum.photos/seed/mango/600/600', 35.00, 1, 35.00, '2026-10-15 08:38:00');

INSERT IGNORE INTO `mall_order_refund` (`id`, `refund_no`, `order_id`, `order_no`, `user_id`, `amount`, `type`, `reason`,
                                        `status`, `audit_remark`, `applied_at`, `handled_at`, `handled_by`)
VALUES (3001, 'RF20261024000001', 1007, 'EM20261024000007', 1, 119.80, 'return', '坚果受潮，口感发软，申请退货退款',
        0, NULL, '2026-10-25 09:10:00', NULL, NULL),
       (3002, 'RF20261025000002', 1002, 'EM20261021000002', 2, 59.90, 'refund', '多拍了一件坚果，仅退这一件',
        1, '同意退款，等待财务打款', '2026-10-25 14:20:00', '2026-10-25 15:00:00', 1),
       (3003, 'RF20261019000003', 1005, 'EM20261018000005', 1, 19.90, 'refund', '蛋卷碎了一半', 2,
        '商品已签收超过 7 天且无质量问题，暂不支持退款', '2026-10-19 20:10:00', '2026-10-20 09:30:00', 1),
       (3004, 'RF20261017000004', 1008, 'EM20261015000008', 2, 35.00, 'return', '芒果熟过头了，有两颗坏果', 3,
        '已退款至原支付账户', '2026-10-17 11:00:00', '2026-10-17 16:40:00', 1);

INSERT IGNORE INTO `mall_order_comment` (`id`, `order_id`, `order_no`, `goods_id`, `goods_name`, `goods_image`, `user_id`,
                                         `user_nickname`, `score`, `content`, `images`, `reply`, `replied_at`, `is_show`,
                                         `commented_at`)
VALUES (4001, 1005, 'EM20261018000005', 4, '手工蛋卷 500g', 'https://picsum.photos/seed/cookie/600/600', 1, '微信用户', 5,
        '蛋卷很脆，奶香味足，包装也没有碎，回购了三次', 'https://picsum.photos/seed/comment-1/400/400',
        '感谢支持，欢迎常来逛逛～', '2026-10-22 09:00:00', 1, '2026-10-21 19:00:00'),
       (4002, 1008, 'EM20261015000008', 2, '海南贵妃芒 3 斤装', 'https://picsum.photos/seed/mango/600/600', 2, '山野小食客', 4,
        '芒果甜度不错，个头也均匀，就是物流慢了一天', 'https://picsum.photos/seed/comment-2/400/400', NULL, NULL, 1,
        '2026-10-17 20:15:00'),
       (4003, 1003, 'EM20261022000003', 2, '海南贵妃芒 3 斤装', 'https://picsum.photos/seed/mango/600/600', 1, '微信用户', 2,
        '有两个已经软烂了，体验一般', NULL, NULL, NULL, 0, '2026-10-24 10:30:00');
