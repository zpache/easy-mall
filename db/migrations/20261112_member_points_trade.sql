-- =============================================================
-- 增量脚本：订单消费打通会员积分与累计消费
-- 背景：此前订单支付后只改订单状态，m_member 的 points / total_spend /
--      order_count 从未更新，会员等级也不会随消费升级。
-- 内容：
--   1. o_order 增加 points_awarded（是否已发放积分，幂等标记）与 points_amount（本单发放积分）
--   2. sys_config 增加 member 分组：积分开关 + 发放比例
--   3. 按已支付订单（status 1/2/3）重算会员累计消费与订单数，并按门槛校正等级
--   4. 历史已支付订单标记为「积分已处理」：不补发积分流水，避免与现有演示积分叠加重复
-- 用法：mysql -uroot -p easy_mall < db/migrations/20261112_member_points_trade.sql
-- 说明：加字段带存在性判断，数据回填可重复执行（重算而非累加）。
-- =============================================================

-- ------------------------------------------------------------
-- 1. o_order 增加积分发放字段
-- ------------------------------------------------------------
SET @col_exists := (SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'o_order'
                      AND COLUMN_NAME = 'points_awarded');
SET @ddl := IF(@col_exists = 0,
               'ALTER TABLE `o_order` ADD COLUMN `points_awarded` tinyint NOT NULL DEFAULT 0 COMMENT ''是否已发放会员积分：0 未发放，1 已发放'' AFTER `cancel_time`, ADD COLUMN `points_amount` int NOT NULL DEFAULT 0 COMMENT ''本单发放积分'' AFTER `points_awarded`',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2. 会员积分规则（存 sys_config，后台「会员管理 - 积分管理」可改）
-- ------------------------------------------------------------
INSERT INTO `sys_config` (`group_code`, `config_key`, `config_value`, `value_type`, `is_secret`, `remark`)
VALUES ('member', 'pointsEnabled', 'true', 'boolean', 0, '订单支付后是否发放积分'),
       ('member', 'pointsRate', '1', 'number', 0, '每消费 1 元发放的积分数')
ON DUPLICATE KEY UPDATE `remark` = VALUES(`remark`);

-- ------------------------------------------------------------
-- 3. 重算会员累计消费与订单数（口径：status 1/2/3 视为已付款）
-- ------------------------------------------------------------
UPDATE `m_member` m
SET m.`total_spend` = IFNULL((SELECT SUM(o.`pay_amount`)
                              FROM `o_order` o
                              WHERE o.`user_id` = m.`id`
                                AND o.`status` IN (1, 2, 3)
                                AND o.`deleted` = 0), 0),
    m.`order_count` = (SELECT COUNT(*)
                       FROM `o_order` o
                       WHERE o.`user_id` = m.`id`
                         AND o.`status` IN (1, 2, 3)
                         AND o.`deleted` = 0)
WHERE m.`deleted` = 0;

-- 等级按累计消费门槛校正（只升不降：目标等级门槛高于当前等级门槛才升级，
-- 避免把运营手工调高的等级 / 历史演示数据降级）
UPDATE `m_member` m
    JOIN `m_member_level` cur ON cur.`id` = m.`level_id`
SET m.`level_id` = (SELECT l.`id`
                    FROM `m_member_level` l
                    WHERE l.`status` = 1
                      AND l.`deleted` = 0
                      AND l.`threshold` <= m.`total_spend`
                    ORDER BY l.`threshold` DESC
                    LIMIT 1)
WHERE m.`deleted` = 0
  AND (SELECT l2.`threshold`
       FROM `m_member_level` l2
       WHERE l2.`status` = 1
         AND l2.`deleted` = 0
         AND l2.`threshold` <= m.`total_spend`
       ORDER BY l2.`threshold` DESC
       LIMIT 1) > cur.`threshold`;

-- ------------------------------------------------------------
-- 4. 历史已支付订单标记为已处理（不补发积分）
-- ------------------------------------------------------------
UPDATE `o_order`
SET `points_awarded` = 1,
    `points_amount`  = FLOOR(`pay_amount` * 1)
WHERE `status` IN (1, 2, 3)
  AND `points_awarded` = 0;

SELECT id, nickname, level_id, points, total_spend, order_count FROM `m_member` WHERE `deleted` = 0 ORDER BY id;
