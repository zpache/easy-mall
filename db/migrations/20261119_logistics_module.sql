-- =============================================================
-- 增量脚本：物流查询 - 快递100 接入
-- 背景：订单发货只存快递公司 + 快递单号，看不到物流轨迹。
--      新增 sys_config 物流分组（快递100 customer / key / 开关），
--      管理后台订单详情与小程序订单详情按需调用快递100实时查询。
-- 用法：mysql -uroot -p easy_mall < db/migrations/20261119_logistics_module.sql
-- 说明：表 IF NOT EXISTS、INSERT ... ON DUPLICATE KEY，可重复执行。
-- =============================================================

USE easy_mall;

-- ------------------------------------------------------------
-- 1. 物流配置默认值（key 未填时开关强制无效，接口返回明确提示）
-- ------------------------------------------------------------
INSERT INTO `sys_config` (`group_code`, `config_key`, `config_value`, `value_type`, `is_secret`, `remark`)
VALUES ('logistics', 'kuaidi100Enabled', 'false', 'boolean', 0, '是否启用快递100物流查询'),
       ('logistics', 'kuaidi100Customer', '', 'string', 0, '快递100 授权客户编号（poll 查询必填）'),
       ('logistics', 'kuaidi100Key', '', 'string', 1, '快递100 授权密钥，用于查询签名')
ON DUPLICATE KEY UPDATE `remark` = VALUES(`remark`);

-- ------------------------------------------------------------
-- 2. 菜单：商城设置 / 物流配置
-- ------------------------------------------------------------
INSERT INTO `sys_menu` (`id`, `pid`, `name`, `title`, `type`, `auth_code`, `path`, `component`, `icon`, `order_no`,
                        `status`)
VALUES (73, 70, 'SettingsLogistics', '物流配置', 'menu', 'settings:logistics:list', '/settings/logistics',
        '/settings/logistics/index', 'lucide:truck', 3, 1)
ON DUPLICATE KEY UPDATE `title`     = VALUES(`title`),
                        `path`      = VALUES(`path`),
                        `component` = VALUES(`component`),
                        `icon`      = VALUES(`icon`),
                        `order_no`  = VALUES(`order_no`);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 73);
