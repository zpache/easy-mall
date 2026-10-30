-- 会员等级折扣参与下单 + 退款回滚会员权益
-- 1. 订单记录会员折扣金额与下单时的等级名称快照
-- 2. 订单记录已退款金额与已回滚积分，用于部分退款与幂等控制
-- 3. 会员配置新增「等级折扣开关」

ALTER TABLE `o_order`
    ADD COLUMN `member_discount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00
        COMMENT '会员等级折扣优惠金额' AFTER `coupon_amount`,
    ADD COLUMN `member_level_name` VARCHAR(50) DEFAULT NULL
        COMMENT '下单时会员等级名称快照' AFTER `member_discount`,
    ADD COLUMN `refunded_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00
        COMMENT '已退款金额累计（含部分退款）' AFTER `pay_amount`,
    ADD COLUMN `points_reverted` INT NOT NULL DEFAULT 0
        COMMENT '已因退款回滚的积分' AFTER `points_amount`;

INSERT INTO `sys_config` (`group_code`, `config_key`, `config_value`, `value_type`, `is_secret`, `remark`, `created_at`, `updated_at`)
VALUES ('member', 'levelDiscountEnabled', 'true', 'boolean', 0, '是否启用会员等级折扣（下单按等级折扣率优惠商品金额）', NOW(), NOW())
ON DUPLICATE KEY UPDATE `updated_at` = NOW();
