-- 微信退款：记录微信退款单号，便于对账与排查
-- 用法：mysql -uroot -p easy_mall < db/migrations/20261115_refund_wx.sql

ALTER TABLE `o_order_refund`
    ADD COLUMN `wx_refund_no` VARCHAR(64) DEFAULT NULL
        COMMENT '微信支付退款单号（模拟退款为空）' AFTER `refund_no`;
