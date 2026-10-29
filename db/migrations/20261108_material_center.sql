-- =============================================================
-- 增量脚本：素材中心
-- 背景：sys_file 仅记录 COS 直传结果，缺少名称列，本地上传不登记；
--      素材中心需要按名称检索、重命名与删除管理能力。
-- 用法：mysql -uroot -p easy_mall < db/migrations/20261108_material_center.sql
-- =============================================================

-- 素材名称（默认取原文件名，可重命名）
ALTER TABLE sys_file
    ADD COLUMN name varchar(200) NULL DEFAULT NULL COMMENT '素材名称' AFTER object_key;

-- 存量数据回填：以对象键尾段作为名称
UPDATE sys_file SET name = SUBSTRING_INDEX(object_key, '/', -1) WHERE name IS NULL;

-- ------------------------------------------------------------
-- 菜单：商城装修 / 素材中心
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `pid`, `name`, `title`, `type`, `auth_code`, `path`, `component`, `icon`,
                                `order_no`, `status`)
VALUES (63, 60, 'DecorateMaterial', '素材中心', 'menu', 'decorate:material:list', '/decorate/material', '/decorate/material/index',
        'lucide:images', 3, 1);

