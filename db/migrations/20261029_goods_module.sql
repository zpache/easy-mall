-- ============================================================
-- 商品管理模块：品牌表、库存流水表、商品品牌/预警字段、菜单与授权
-- 幂等：可重复执行
-- ============================================================

-- ------------------------------------------------------------
-- 1. 品牌
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mall_brand`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       varchar(50) NOT NULL COMMENT '品牌名称',
    `logo`       varchar(255)         DEFAULT NULL COMMENT '品牌 LOGO',
    `sort`       int         NOT NULL DEFAULT 0 COMMENT '排序值，越小越前',
    `status`     tinyint     NOT NULL DEFAULT 1 COMMENT '1 启用，0 停用',
    `remark`     varchar(255)         DEFAULT NULL COMMENT '备注',
    `created_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` bigint               DEFAULT NULL COMMENT '创建人 ID',
    `updated_by` bigint               DEFAULT NULL COMMENT '最后修改人 ID',
    `version`    int         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`    tinyint     NOT NULL DEFAULT 0 COMMENT '0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_brand_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品品牌';

-- ------------------------------------------------------------
-- 2. 库存流水
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mall_stock_log`
(
    `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `goods_id`     bigint      NOT NULL COMMENT '商品 ID',
    `type`         varchar(16) NOT NULL COMMENT '类型：in 入库 / out 出库 / check 盘点',
    `change_qty`   int         NOT NULL COMMENT '变动数量，正数增加、负数减少',
    `before_stock` int         NOT NULL COMMENT '变动前库存',
    `after_stock`  int         NOT NULL COMMENT '变动后库存',
    `remark`       varchar(255)         DEFAULT NULL COMMENT '备注',
    `created_at`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by`   bigint               DEFAULT NULL COMMENT '操作人 ID',
    PRIMARY KEY (`id`),
    KEY `idx_stock_log_goods` (`goods_id`),
    KEY `idx_stock_log_created` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品库存流水';

-- ------------------------------------------------------------
-- 3. mall_goods 增补品牌与预警值（MySQL 无 ADD COLUMN IF NOT EXISTS，用动态 SQL 保证幂等）
-- ------------------------------------------------------------
SET @has_brand_id := (SELECT COUNT(*)
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = DATABASE()
                        AND TABLE_NAME = 'mall_goods'
                        AND COLUMN_NAME = 'brand_id');
SET @sql := IF(@has_brand_id = 0,
               'ALTER TABLE `mall_goods` ADD COLUMN `brand_id` bigint DEFAULT NULL COMMENT ''品牌 ID'' AFTER `category_id`',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_stock_warn := (SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'mall_goods'
                          AND COLUMN_NAME = 'stock_warn');
SET @sql := IF(@has_stock_warn = 0,
               'ALTER TABLE `mall_goods` ADD COLUMN `stock_warn` int NOT NULL DEFAULT 10 COMMENT ''库存预警值'' AFTER `stock`',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_brand_idx := (SELECT COUNT(*)
                       FROM information_schema.STATISTICS
                       WHERE TABLE_SCHEMA = DATABASE()
                         AND TABLE_NAME = 'mall_goods'
                         AND INDEX_NAME = 'idx_goods_brand');
SET @sql := IF(@has_brand_idx = 0,
               'ALTER TABLE `mall_goods` ADD KEY `idx_goods_brand` (`brand_id`)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 4. 品牌种子数据
-- ------------------------------------------------------------
INSERT IGNORE INTO `mall_brand` (`id`, `name`, `logo`, `sort`, `status`, `remark`)
VALUES (1, '果然鲜', 'https://picsum.photos/seed/brand-fruit/120/120', 1, 1, '水果生鲜自有品牌'),
       (2, '坚果日记', 'https://picsum.photos/seed/brand-nut/120/120', 2, 1, '休闲零食'),
       (3, '山野咖啡', 'https://picsum.photos/seed/brand-coffee/120/120', 3, 1, '咖啡与茶饮'),
       (4, '优鲜生活', 'https://picsum.photos/seed/brand-life/120/120', 4, 1, '综合食品');

-- 为已有商品回填品牌（仅回填空值）
UPDATE `mall_goods` g
    JOIN `mall_category` c ON c.`id` = g.`category_id`
SET g.`brand_id` = CASE
                       WHEN c.`id` = 1 THEN 1
                       WHEN c.`id` = 2 THEN 2
                       WHEN c.`id` = 3 THEN 3
                       ELSE 4
    END
WHERE g.`brand_id` IS NULL;

-- ------------------------------------------------------------
-- 5. 菜单：商品管理（目录 + 4 个子菜单）
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `pid`, `name`, `title`, `type`, `auth_code`, `path`, `component`, `icon`,
                                `order_no`, `status`)
VALUES (20, 0, 'Goods', '商品管理', 'catalog', NULL, '/goods', NULL, 'lucide:shopping-bag', 10, 1),
       (21, 20, 'GoodsList', '商品列表', 'menu', 'goods:goods:list', '/goods/list', '/goods/list/index',
        'lucide:package', 1, 1),
       (22, 20, 'GoodsCategory', '商品分类', 'menu', 'goods:category:list', '/goods/category', '/goods/category/index',
        'lucide:folder-tree', 2, 1),
       (23, 20, 'GoodsBrand', '品牌管理', 'menu', 'goods:brand:list', '/goods/brand', '/goods/brand/index',
        'lucide:badge-check', 3, 1),
       (24, 20, 'GoodsStock', '库存管理', 'menu', 'goods:stock:list', '/goods/stock', '/goods/stock/index',
        'lucide:warehouse', 4, 1);

-- 超级管理员、运营人员均可见商品管理
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 20), (1, 21), (1, 22), (1, 23), (1, 24),
       (2, 20), (2, 21), (2, 22), (2, 23), (2, 24);
