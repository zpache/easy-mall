-- =============================================================
-- 增量脚本：确认收货 + 微信支付/退款参数
-- 背景：1) 订单状态缺「待收货 → 已完成」的落点，补自动确认收货天数配置；
--      2) 接入微信支付 v3 需要「微信支付公钥」验签回调、以及退款回调地址。
-- 用法：mysql -uroot -p easy_mall < db/migrations/20261114_pay_receive.sql
-- 说明：INSERT ... ON DUPLICATE KEY，可重复执行；不覆盖已有配置值。
-- =============================================================

INSERT INTO `sys_config` (`group_code`, `config_key`, `config_value`, `value_type`, `is_secret`, `remark`)
VALUES ('payment', 'autoReceiveDays', '15', 'number', 0, '发货后自动确认收货天数，0 表示不自动完成'),
       ('payment', 'wxpayPublicKey', '', 'text', 1,
        '微信支付公钥（pub_key.pem 内容），用于验证回调签名；未配置则回调一律拒绝'),
       ('payment', 'wxpayPublicKeyId', '', 'string', 0, '微信支付公钥 ID（PUB_KEY_ID_ 开头）'),
       ('payment', 'refundNotifyUrl', '', 'string', 0, '退款结果回调地址，留空则只依赖主动查询')
ON DUPLICATE KEY UPDATE `remark` = VALUES(`remark`);
