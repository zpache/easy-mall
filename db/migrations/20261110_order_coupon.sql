-- 订单支持优惠券：记录使用的券模板与抵扣金额（订单表为 o_order）
ALTER TABLE o_order
    ADD COLUMN coupon_id BIGINT NULL COMMENT '使用的优惠券模板 id' AFTER freight_amount,
    ADD COLUMN coupon_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠券抵扣金额' AFTER coupon_id;
