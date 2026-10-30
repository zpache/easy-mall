-- =============================================================
-- 增量脚本：主题换肤补「小程序默认配色」预设
-- 背景：小程序端此前有一套写死在代码/SCSS 里的默认配色（主色 #ff5a1f、价格 #e02020、
--       页面底 #f6f7f9、主文字 #1f1f1f、次要文字 #8c8c8c）。本次把它收进主题换肤体系，
--       作为一套可管理、可切换的预设主题，保证换肤前后小程序视觉一致。
-- 说明：脚本只新增预设，不抢占线上主题；仅当库里还没有任何线上主题时才把它设为线上，
--       避免执行脚本的瞬间改变 C 端当前配色。
-- 用法：mysql -uroot -p easy_mall < db/migrations/20261110_theme_default_palette.sql
-- =============================================================

-- 原「活力橙（默认）」不再是唯一的默认说法，去掉名称里的默认标记，避免后台出现两个默认
UPDATE dc_theme
SET name = '活力橙'
WHERE name = '活力橙（默认）'
  AND deleted = 0;

-- 小程序历史默认配色（与 miniprogram/src/uni.scss 的内置兜底一致）
INSERT INTO dc_theme (name, config_json, published_json, is_active, remark)
SELECT '小程序默认配色（经典橙）',
       '{"primaryColor":"#ff5a1f","pageBgColor":"#f6f7f9","priceColor":"#e02020","textColor":"#1f1f1f","subTextColor":"#8c8c8c"}',
       '{"primaryColor":"#ff5a1f","pageBgColor":"#f6f7f9","priceColor":"#e02020","textColor":"#1f1f1f","subTextColor":"#8c8c8c"}',
       0,
       '预设主题：小程序换肤前的默认配色'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT id FROM dc_theme WHERE name = '小程序默认配色（经典橙）' AND deleted = 0) t
);

-- 兜底：库里一套线上主题都没有时，用这套预设顶上，避免小程序端一直走内置默认值
UPDATE dc_theme
SET is_active = 1,
    published_json = config_json
WHERE deleted = 0
  AND name = '小程序默认配色（经典橙）'
  AND NOT EXISTS (
    SELECT 1 FROM (SELECT id FROM dc_theme WHERE is_active = 1 AND deleted = 0) t
);
