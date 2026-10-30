-- 评价唯一性：同一订单同一商品只允许评价一次
-- 先清理历史重复数据（同一订单+商品保留 id 最小的一条），再补唯一索引。
-- 重复执行安全：索引已存在时脚本跳过。

DELETE c1
FROM `o_order_comment` c1
         JOIN `o_order_comment` c2
              ON c1.order_id = c2.order_id
                  AND c1.goods_id = c2.goods_id
                  AND c1.id > c2.id;

SET @idx_exists := (SELECT COUNT(*)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'o_order_comment'
                      AND index_name = 'uk_comment_order_goods');

SET @ddl := IF(@idx_exists = 0,
               'ALTER TABLE `o_order_comment` ADD UNIQUE KEY `uk_comment_order_goods` (`order_id`, `goods_id`)',
               'DO 0');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
