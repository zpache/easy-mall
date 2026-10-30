-- =============================================================
-- 增量脚本：修复 o_order_promotion 缺失审计列
-- 背景：20261030_order_promotion.sql 建表时只有 created_at，
--      而 api 端 OrderPromotion 实体继承 BaseEntity（约定
--      created_at/updated_at/created_by/updated_by/version/deleted），
--      小程序订单列表/详情查询报 Unknown column 'updated_at'。
--      补齐 5 列，与 BaseEntity 约定对齐。
-- 用法：mysql -uroot -p easy_mall < db/migrations/20261119_fix_order_promotion_columns.sql
-- 说明：按 information_schema 判断列是否存在，可重复执行。
-- =============================================================

USE easy_mall;

-- updated_at（NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP）
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'easy_mall' AND TABLE_NAME = 'o_order_promotion' AND COLUMN_NAME = 'updated_at') = 0,
  'ALTER TABLE `o_order_promotion` ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`',
  'SELECT ''updated_at 已存在，跳过'' AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- created_by
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'easy_mall' AND TABLE_NAME = 'o_order_promotion' AND COLUMN_NAME = 'created_by') = 0,
  'ALTER TABLE `o_order_promotion` ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT ''创建人 ID'' AFTER `updated_at`',
  'SELECT ''created_by 已存在，跳过'' AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- updated_by
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'easy_mall' AND TABLE_NAME = 'o_order_promotion' AND COLUMN_NAME = 'updated_by') = 0,
  'ALTER TABLE `o_order_promotion` ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT ''最后修改人 ID'' AFTER `created_by`',
  'SELECT ''updated_by 已存在，跳过'' AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- version（乐观锁，NOT NULL DEFAULT 0）
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'easy_mall' AND TABLE_NAME = 'o_order_promotion' AND COLUMN_NAME = 'version') = 0,
  'ALTER TABLE `o_order_promotion` ADD COLUMN `version` int NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `updated_by`',
  'SELECT ''version 已存在，跳过'' AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- deleted（逻辑删除，MyBatis-Plus 查询会带 deleted=0）
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'easy_mall' AND TABLE_NAME = 'o_order_promotion' AND COLUMN_NAME = 'deleted') = 0,
  'ALTER TABLE `o_order_promotion` ADD COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT ''0 未删除，1 已删除'' AFTER `version`',
  'SELECT ''deleted 已存在，跳过'' AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
