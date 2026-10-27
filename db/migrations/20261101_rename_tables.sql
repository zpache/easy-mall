-- =====================================================================
-- 20261101 表名规范化：去掉 mall_ 前缀，按模块使用 o_ / m_ 前缀
--   订单模块：o_ 前缀（mall_order → o_order 等）
--   会员模块：m_ 前缀（app_user → m_member 等）
--   商品模块：保留 mall_ 前缀（mall_goods / mall_category / mall_brand / mall_stock_log）
--   sys_* / cart_item / user_address 不在本次调整范围
-- 幂等：通过 information_schema 判断旧表存在且新表不存在才执行 RENAME
-- =====================================================================

DROP PROCEDURE IF EXISTS rename_table_if_exists;

DELIMITER $$
CREATE PROCEDURE rename_table_if_exists(IN old_name VARCHAR(64), IN new_name VARCHAR(64))
BEGIN
    DECLARE old_exists INT DEFAULT 0;
    DECLARE new_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO old_exists FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = old_name;
    SELECT COUNT(*) INTO new_exists FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = new_name;
    IF old_exists = 1 AND new_exists = 0 THEN
        SET @ddl = CONCAT('RENAME TABLE `', old_name, '` TO `', new_name, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ---------- 订单模块：o_ 前缀 ----------
CALL rename_table_if_exists('mall_order', 'o_order');
CALL rename_table_if_exists('order_item', 'o_order_item');
CALL rename_table_if_exists('mall_order_refund', 'o_order_refund');
CALL rename_table_if_exists('mall_order_comment', 'o_order_comment');

-- ---------- 会员模块：m_ 前缀 ----------
CALL rename_table_if_exists('app_user', 'm_member');
CALL rename_table_if_exists('mall_member_level', 'm_member_level');
CALL rename_table_if_exists('mall_points_log', 'm_points_log');
CALL rename_table_if_exists('mall_member_tag', 'm_member_tag');
CALL rename_table_if_exists('mall_member_user_tag', 'm_member_user_tag');

DROP PROCEDURE IF EXISTS rename_table_if_exists;
