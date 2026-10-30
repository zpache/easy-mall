-- 移除商品分类的图标字段：管理后台与小程序均未使用
USE easy_mall;

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'easy_mall'
    AND TABLE_NAME = 'mall_category'
    AND COLUMN_NAME = 'icon'
);

SET @sql = IF(@col_exists > 0,
  'ALTER TABLE `mall_category` DROP COLUMN `icon`',
  'SELECT ''mall_category.icon 不存在，跳过'' AS note');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
