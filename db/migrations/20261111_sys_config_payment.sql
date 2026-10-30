-- =============================================================
-- 增量脚本：商城设置 - 支付配置（第一期）
-- 背景：原有支付只有小程序端「模拟支付」接口（PUT /orders/{id}/pay），
--      没有任何可维护的支付参数。本期新增通用配置表 sys_config，
--      用于按「分组 + 键」存放商城配置（支付 / 小程序 / 交易等），
--      并落地微信支付 v3 的商户参数维护 + 参数自检 + 模拟支付开关。
-- 用法：mysql -uroot -p easy_mall < db/migrations/20261111_sys_config_payment.sql
-- 说明：表 IF NOT EXISTS、数据 INSERT ... ON DUPLICATE KEY，可重复执行。
-- =============================================================

-- ------------------------------------------------------------
-- 1. 通用配置表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_config`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_code`   varchar(50)  NOT NULL COMMENT '配置分组：payment 支付 / miniprogram 小程序 / trade 交易',
    `config_key`   varchar(100) NOT NULL COMMENT '配置项键，分组内唯一',
    `config_value` text COMMENT '配置值，统一按字符串存储，按 value_type 解析',
    `value_type`   varchar(20)  NOT NULL DEFAULT 'string' COMMENT '值类型：string/number/boolean/text',
    `is_secret`    tinyint      NOT NULL DEFAULT 0 COMMENT '1 敏感值（密钥/私钥），返回前端时脱敏',
    `remark`       varchar(255)          DEFAULT NULL COMMENT '配置项说明',
    `created_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`   bigint                DEFAULT NULL COMMENT '创建人 ID',
    `updated_by`   bigint                DEFAULT NULL COMMENT '最后修改人 ID',
    `version`      int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`      tinyint      NOT NULL DEFAULT 0 COMMENT '0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_config_group_key` (`group_code`, `config_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统配置（按分组存放的键值配置）';

-- ------------------------------------------------------------
-- 2. 支付配置默认值
--    默认保持「模拟支付开启、微信支付关闭」，与改造前行为一致，
--    避免升级后小程序无法付款。
-- ------------------------------------------------------------
INSERT INTO `sys_config` (`group_code`, `config_key`, `config_value`, `value_type`, `is_secret`, `remark`)
VALUES ('payment', 'wxpayEnabled', 'false', 'boolean', 0, '是否启用微信支付渠道'),
       ('payment', 'mockPayEnabled', 'true', 'boolean', 0, '是否启用模拟支付（开发联调 / 未接入微信支付时使用）'),
       ('payment', 'appId', '', 'string', 0, '小程序 AppID，微信支付下单必填'),
       ('payment', 'mchId', '', 'string', 0, '微信支付商户号'),
       ('payment', 'mchSerialNo', '', 'string', 0, '商户 API 证书序列号'),
       ('payment', 'apiV3Key', '', 'string', 1, 'APIv3 密钥（32 位），用于回调报文解密'),
       ('payment', 'privateKey', '', 'text', 1, '商户 API 私钥（apiclient_key.pem 内容），用于请求签名'),
       ('payment', 'notifyUrl', '', 'string', 0, '支付结果回调地址，必须为 https'),
       ('payment', 'orderPayTimeoutMinutes', '30', 'number', 0, '订单支付超时时长（分钟），超时自动关单')
ON DUPLICATE KEY UPDATE `remark` = VALUES(`remark`);

-- ------------------------------------------------------------
-- 3. 菜单：商城设置 / 支付配置
-- ------------------------------------------------------------
INSERT INTO `sys_menu` (`id`, `pid`, `name`, `title`, `type`, `auth_code`, `path`, `component`, `icon`, `order_no`,
                        `status`)
VALUES (72, 70, 'SettingsPayment', '支付配置', 'menu', 'settings:payment:list', '/settings/payment',
        '/settings/payment/index', 'lucide:credit-card', 2, 1)
ON DUPLICATE KEY UPDATE `title`     = VALUES(`title`),
                        `path`      = VALUES(`path`),
                        `component` = VALUES(`component`),
                        `icon`      = VALUES(`icon`),
                        `order_no`  = VALUES(`order_no`);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 72);
