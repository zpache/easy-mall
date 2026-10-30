-- 评价审核：用户提交后进入待审核，管理端审核通过后才对其他用户展示
-- 用法：mysql -uroot -p easy_mall < db/migrations/20261117_comment_audit.sql
-- 重复执行安全：列 / 索引已存在时跳过；存量数据初始化只在首次加列时执行一次，
-- 避免重跑脚本把后产生的待审核评价刷成已通过。

SET @has_col := (SELECT COUNT(*)
                 FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'o_order_comment'
                   AND column_name = 'audit_status');

SET @ddl := IF(@has_col = 0,
               'ALTER TABLE `o_order_comment`
                    ADD COLUMN `audit_status` TINYINT NOT NULL DEFAULT 0
                        COMMENT ''审核状态：0待审核 1已通过 2已拒绝'' AFTER `is_show`,
                    ADD COLUMN `audit_remark` VARCHAR(200) DEFAULT NULL
                        COMMENT ''审核备注（拒绝原因）'' AFTER `audit_status`,
                    ADD COLUMN `audit_at` DATETIME DEFAULT NULL
                        COMMENT ''审核时间'' AFTER `audit_remark`',
               'DO 0');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量评价视为已通过：上线前已经展示过的评价不能突然从小程序消失
SET @init := IF(@has_col = 0,
                'UPDATE `o_order_comment`
                    SET `audit_status` = 1,
                        `audit_at` = COALESCE(`commented_at`, NOW())',
                'DO 0');

PREPARE stmt2 FROM @init;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @idx_exists := (SELECT COUNT(*)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'o_order_comment'
                      AND index_name = 'idx_comment_audit');

SET @idx_ddl := IF(@idx_exists = 0,
                   'ALTER TABLE `o_order_comment` ADD KEY `idx_comment_audit` (`audit_status`, `goods_id`)',
                   'DO 0');

PREPARE stmt3 FROM @idx_ddl;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;
