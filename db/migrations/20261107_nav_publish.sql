-- 导航装修（dc_nav）增加"草稿 / 已发布"双份配置：
-- 保存只写草稿三列（tabbar_json 等），点「发布」后拷贝到 published 三列；
-- 小程序端 /decorate/nav 只读 published 列，发布前线上配置不受编辑影响。
-- 存量数据回填：当前草稿即线上生效内容，直接作为初始发布快照，行为保持连续。
ALTER TABLE `dc_nav`
    ADD COLUMN `tabbar_published_json` longtext DEFAULT NULL COMMENT '已发布底部 TabBar（小程序读这列）' AFTER `tabbar_json`,
    ADD COLUMN `navbar_published_json` longtext DEFAULT NULL COMMENT '已发布顶部导航（小程序读这列）' AFTER `navbar_json`,
    ADD COLUMN `float_published_json`  longtext DEFAULT NULL COMMENT '已发布快捷入口（小程序读这列）' AFTER `float_json`;

UPDATE `dc_nav`
SET `tabbar_published_json` = `tabbar_json`,
    `navbar_published_json` = `navbar_json`,
    `float_published_json`  = `float_json`
WHERE `tabbar_published_json` IS NULL;
