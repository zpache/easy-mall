-- 订单支付渠道记录：支付成功时落库，供退款路由与对账使用
-- pay_type: 0 模拟支付 / 1 微信支付 / 2 支付宝（预留）
-- pay_channel_no: 渠道方交易号（微信 transaction_id；模拟支付为空）
-- 存量已支付订单不回填（历史数据无法区分当时走的是模拟还是微信，退款逻辑按 NULL 兼容处理）
ALTER TABLE o_order
    ADD COLUMN pay_type tinyint NULL DEFAULT NULL COMMENT '支付方式：0 模拟支付，1 微信支付，2 支付宝（预留）' AFTER pay_amount,
    ADD COLUMN pay_channel_no varchar(64) NULL DEFAULT NULL COMMENT '渠道交易号（微信 transaction_id 等）' AFTER pay_type;
