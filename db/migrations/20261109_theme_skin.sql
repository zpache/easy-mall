-- =============================================================
-- 增量脚本：商城主题换肤
-- 背景：全局换肤配置，草稿 / 发布双快照 + 单启用，与页面装修同口径：
--      保存只写草稿列，「启用」发布快照并切换线上主题，小程序端只读已启用主题的发布快照。
-- 用法：mysql -uroot -p easy_mall < db/migrations/20261109_theme_skin.sql
-- =============================================================

CREATE TABLE IF NOT EXISTS dc_theme (
    id              bigint       NOT NULL AUTO_INCREMENT,
    name            varchar(100) NOT NULL COMMENT '主题名称',
    config_json     longtext     NULL COMMENT '主题配置草稿 JSON',
    published_json  longtext     NULL COMMENT '主题配置发布快照 JSON',
    is_active       tinyint      NOT NULL DEFAULT 0 COMMENT '是否线上主题：0-否 1-是（全局唯一）',
    remark          varchar(255) NULL,
    created_at      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      bigint       NULL,
    updated_by      bigint       NULL,
    version         int          NOT NULL DEFAULT 0,
    deleted         tinyint      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='商城装修主题';

-- 预设主题；首个为线上默认主题
INSERT INTO dc_theme (name, config_json, published_json, is_active, remark) VALUES
('活力橙（默认）',
 '{"primaryColor":"#ff6b00","pageBgColor":"#f5f6f7","priceColor":"#ff4d1f","textColor":"#1f1f1f","subTextColor":"#999999"}',
 '{"primaryColor":"#ff6b00","pageBgColor":"#f5f6f7","priceColor":"#ff4d1f","textColor":"#1f1f1f","subTextColor":"#999999"}',
 1, '预设主题'),
('清新绿',
 '{"primaryColor":"#07c160","pageBgColor":"#f4f8f5","priceColor":"#07c160","textColor":"#1f1f1f","subTextColor":"#8a938c"}',
 '{"primaryColor":"#07c160","pageBgColor":"#f4f8f5","priceColor":"#07c160","textColor":"#1f1f1f","subTextColor":"#8a938c"}',
 0, '预设主题'),
('静谧蓝',
 '{"primaryColor":"#1677ff","pageBgColor":"#f4f6fa","priceColor":"#ff4d1f","textColor":"#1f2329","subTextColor":"#8c919f"}',
 '{"primaryColor":"#1677ff","pageBgColor":"#f4f6fa","priceColor":"#ff4d1f","textColor":"#1f2329","subTextColor":"#8c919f"}',
 0, '预设主题'),
('热力红',
 '{"primaryColor":"#e5394b","pageBgColor":"#faf3f3","priceColor":"#e5394b","textColor":"#26191b","subTextColor":"#9c8b8d"}',
 '{"primaryColor":"#e5394b","pageBgColor":"#faf3f3","priceColor":"#e5394b","textColor":"#26191b","subTextColor":"#9c8b8d"}',
 0, '预设主题'),
('深邃紫',
 '{"primaryColor":"#6f42e8","pageBgColor":"#f6f4fb","priceColor":"#ff4d1f","textColor":"#221c33","subTextColor":"#8f8aa3"}',
 '{"primaryColor":"#6f42e8","pageBgColor":"#f6f4fb","priceColor":"#ff4d1f","textColor":"#221c33","subTextColor":"#8f8aa3"}',
 0, '预设主题');

-- ------------------------------------------------------------
-- 菜单：商城装修 / 主题换肤
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `pid`, `name`, `title`, `type`, `auth_code`, `path`, `component`, `icon`,
                                `order_no`, `status`)
VALUES (64, 60, 'DecorateTheme', '主题换肤', 'menu', 'decorate:theme:list', '/decorate/theme', '/decorate/theme/index',
        'lucide:palette', 4, 1);
