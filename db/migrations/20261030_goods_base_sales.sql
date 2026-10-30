-- 商品销量拆分为「基础销量 + 真实销量」
-- base_sales：运营在后台维护的展示基数（种子销量），不随交易变化
-- sales：真实销量，用户下单累加、取消回滚，管理后台以此为准
ALTER TABLE `mall_goods`
    ADD COLUMN `base_sales` INT NOT NULL DEFAULT 0 COMMENT '基础销量（运营维护的展示基数）' AFTER `sales`;

-- 回填：现有 sales 是「种子 + 净下单」的混合值，按有效订单（非取消）累计数量反推拆分
UPDATE `mall_goods` g
SET g.`base_sales` = GREATEST(
        g.`sales` - IFNULL((
            SELECT SUM(oi.`quantity`)
            FROM `o_order_item` oi
            JOIN `o_order` o ON o.`id` = oi.`order_id`
            WHERE oi.`goods_id` = g.`id`
              AND o.`status` <> 4
        ), 0),
        0
    );

-- 真实销量同步为有效订单累计（与拆分口径对齐，保证 基础 + 真实 = 原 sales）
UPDATE `mall_goods` g
SET g.`sales` = IFNULL((
        SELECT SUM(oi.`quantity`)
        FROM `o_order_item` oi
        JOIN `o_order` o ON o.`id` = oi.`order_id`
        WHERE oi.`goods_id` = g.`id`
          AND o.`status` <> 4
    ), 0);

SELECT id, base_sales, sales, base_sales + sales AS total FROM `mall_goods` ORDER BY id;
