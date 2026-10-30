-- ============================================================
-- 满减活动集成下单链路：
--   1) o_order 增加 reduction_amount 满减优惠汇总金额
--   2) 新建 o_order_promotion 订单优惠快照表：订单与参加的每个满减活动 1 对多，
--      落库下单时的活动快照（名称 / 模式 / 门槛 / 优惠 / 命中次数 / 实优惠金额），
--      订单表不直接存 reduction_id，活动后续修改不影响历史订单展示
-- 叠加顺序：商品总价 -> 会员折扣 -> 满减 -> 优惠券（门槛按满减后金额校验）-> 运费
-- 幂等：可重复执行
-- ============================================================

ALTER TABLE `o_order`
    ADD COLUMN `reduction_amount` decimal(10, 2) NOT NULL DEFAULT 0.00
        COMMENT '满减优惠金额（多活动合计），未参加为 0' AFTER `coupon_amount`;

CREATE TABLE IF NOT EXISTS `o_order_promotion`
(
    `id`              bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`        bigint         NOT NULL COMMENT '订单 ID',
    `order_no`        varchar(32)    NOT NULL COMMENT '订单号',
    `promotion_id`    bigint         NOT NULL COMMENT '满减活动 ID',
    `promotion_name`  varchar(100)   NOT NULL COMMENT '活动名称快照',
    `mode`            tinyint        NOT NULL COMMENT '1 阶梯满减 2 每满减（快照）',
    `threshold`       decimal(10, 2) NOT NULL COMMENT '满 X 元（快照）',
    `benefit`         decimal(10, 2) NOT NULL COMMENT '减 Y 元（快照）',
    `times`           int            NOT NULL DEFAULT 1 COMMENT '命中次数（每满减按金额可叠加多次，阶梯为 1）',
    `discount_amount` decimal(10, 2) NOT NULL COMMENT '本单该活动实际优惠金额',
    `created_at`      datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_op_order` (`order_id`),
    KEY `idx_op_order_no` (`order_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单优惠快照（满减）';
