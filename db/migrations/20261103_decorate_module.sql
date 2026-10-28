-- ============================================================
-- 商城装修模块：页面表、菜单与授权、演示数据
-- 幂等：可重复执行
-- ============================================================

-- ------------------------------------------------------------
-- 1. 装修页面（草稿与发布各存一份快照，预览码用于真机扫码）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dc_page`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           varchar(100) NOT NULL COMMENT '页面名称',
    `type`           tinyint      NOT NULL DEFAULT 1 COMMENT '1 首页 2 分类页 3 微页面',
    `status`         tinyint      NOT NULL DEFAULT 0 COMMENT '0 草稿 1 已发布',
    `is_home`        tinyint      NOT NULL DEFAULT 0 COMMENT '是否设为商城首页',
    `preview_code`   varchar(32)           DEFAULT NULL COMMENT '真机预览码',
    `schema_json`    longtext              DEFAULT NULL COMMENT '当前编辑内容（草稿）',
    `published_json` longtext              DEFAULT NULL COMMENT '已发布快照',
    `published_at`   datetime              DEFAULT NULL COMMENT '最近发布时间',
    `remark`         varchar(255)          DEFAULT NULL COMMENT '备注',
    `created_at`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`     bigint                DEFAULT NULL,
    `updated_by`     bigint                DEFAULT NULL,
    `version`        int          NOT NULL DEFAULT 0,
    `deleted`        tinyint      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_page_preview_code` (`preview_code`),
    KEY `idx_page_home` (`is_home`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商城装修页面';

-- ------------------------------------------------------------
-- 2. 菜单：商城装修（目录 + 页面装修）
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `pid`, `name`, `title`, `type`, `auth_code`, `path`, `component`, `icon`,
                                `order_no`, `status`)
VALUES (60, 0, 'Decorate', '商城装修', 'catalog', NULL, '/decorate', NULL, 'lucide:layout-template', 40, 1),
       (61, 60, 'DecoratePage', '页面装修', 'menu', 'decorate:page:list', '/decorate/list', '/decorate/list/index',
        'lucide:panels-top-left', 1, 1);

-- 超级管理员、运营人员均可见商城装修
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 60), (1, 61),
       (2, 60), (2, 61);

-- ------------------------------------------------------------
-- 3. 演示数据：一个已发布的首页
-- schema 结构与前端 widgets 注册表一致：page 配置 + components 数组
-- ------------------------------------------------------------
INSERT IGNORE INTO `dc_page` (`id`, `name`, `type`, `status`, `is_home`, `preview_code`, `schema_json`,
                              `published_json`, `published_at`, `remark`)
VALUES (1, '商城首页', 1, 1, 1, 'DEMOHOME',
        '{"page":{"title":"商城首页","bgColor":"#f5f6f7","bgImage":"","padding":0},"components":[{"id":"c_home_search","type":"search","visible":true,"props":{"placeholder":"搜索商品 / 品牌","radius":18,"align":"left","bgColor":"#ffffff"}},{"id":"c_home_notice","type":"notice","visible":true,"props":{"text":"新客首单立减 10 元，满 99 包邮","icon":"volume","color":"#ff6b00","bgColor":"#fff7e8"}},{"id":"c_home_banner","type":"banner","visible":true,"props":{"height":150,"radius":8,"interval":3,"margin":12,"images":[{"url":"https://picsum.photos/seed/mall-banner-1/750/300","link":"/goods/1"},{"url":"https://picsum.photos/seed/mall-banner-2/750/300","link":"/goods/3"},{"url":"https://picsum.photos/seed/mall-banner-3/750/300","link":"/goods/5"}]}},{"id":"c_home_nav","type":"nav","visible":true,"props":{"columns":5,"items":[{"icon":"https://picsum.photos/seed/nav-1/80/80","text":"新鲜水果","link":"/category/1"},{"icon":"https://picsum.photos/seed/nav-2/80/80","text":"肉禽蛋奶","link":"/category/2"},{"icon":"https://picsum.photos/seed/nav-3/80/80","text":"休闲零食","link":"/category/3"},{"icon":"https://picsum.photos/seed/nav-4/80/80","text":"粮油调味","link":"/category/4"},{"icon":"https://picsum.photos/seed/nav-5/80/80","text":"全部分类","link":"/category"}]}},{"id":"c_home_seckill","type":"seckill","visible":true,"props":{"title":"限时秒杀","subtitle":"每天 10 点开抢","goodsName":"当季赣南脐橙 5 斤装","image":"https://picsum.photos/seed/seckill-1/300/300","price":19.9,"originPrice":29.9,"endTime":"2026-12-31 22:00:00","bgColor":"#fff1f0"}},{"id":"c_home_title","type":"title","visible":true,"props":{"text":"为你推荐","subtitle":"猜你喜欢","align":"left","size":16,"color":"#333333","bold":true,"more":true}},{"id":"c_home_goods","type":"goods","visible":true,"props":{"layout":"grid","columns":2,"count":4,"showName":true,"showPrice":true,"showSales":true,"goods":[{"id":1,"name":"当季赣南脐橙 5 斤装","image":"https://picsum.photos/seed/goods-1/400/400","price":29.9,"sales":1280},{"id":2,"name":"智利车厘子 JJ 级 2 斤","image":"https://picsum.photos/seed/goods-2/400/400","price":89.0,"sales":642},{"id":3,"name":"每日坚果混合装 30 包","image":"https://picsum.photos/seed/goods-3/400/400","price":59.9,"sales":2310},{"id":4,"name":"低温鲜牛乳 950ml x 6","image":"https://picsum.photos/seed/goods-4/400/400","price":69.0,"sales":876}]}},{"id":"c_home_coupon","type":"coupon","visible":true,"props":{"title":"领券中心","bgColor":"#ffffff","coupons":[{"name":"新人专享 10 元券","amount":10,"threshold":0,"validDays":7},{"name":"满 99 减 15","amount":15,"threshold":99,"validDays":30},{"name":"生鲜 9 折券","amount":0,"discount":90,"threshold":50,"validDays":15}]}},{"id":"c_home_shop","type":"shopInfo","visible":true,"props":{"name":"易商城旗舰店","logo":"https://picsum.photos/seed/shop-logo/120/120","desc":"生鲜果蔬 · 次日达","phone":"400-800-1234","bgColor":"#ffffff"}}]}',
        '{"page":{"title":"商城首页","bgColor":"#f5f6f7","bgImage":"","padding":0},"components":[{"id":"c_home_search","type":"search","visible":true,"props":{"placeholder":"搜索商品 / 品牌","radius":18,"align":"left","bgColor":"#ffffff"}},{"id":"c_home_notice","type":"notice","visible":true,"props":{"text":"新客首单立减 10 元，满 99 包邮","icon":"volume","color":"#ff6b00","bgColor":"#fff7e8"}},{"id":"c_home_banner","type":"banner","visible":true,"props":{"height":150,"radius":8,"interval":3,"margin":12,"images":[{"url":"https://picsum.photos/seed/mall-banner-1/750/300","link":"/goods/1"},{"url":"https://picsum.photos/seed/mall-banner-2/750/300","link":"/goods/3"},{"url":"https://picsum.photos/seed/mall-banner-3/750/300","link":"/goods/5"}]}},{"id":"c_home_nav","type":"nav","visible":true,"props":{"columns":5,"items":[{"icon":"https://picsum.photos/seed/nav-1/80/80","text":"新鲜水果","link":"/category/1"},{"icon":"https://picsum.photos/seed/nav-2/80/80","text":"肉禽蛋奶","link":"/category/2"},{"icon":"https://picsum.photos/seed/nav-3/80/80","text":"休闲零食","link":"/category/3"},{"icon":"https://picsum.photos/seed/nav-4/80/80","text":"粮油调味","link":"/category/4"},{"icon":"https://picsum.photos/seed/nav-5/80/80","text":"全部分类","link":"/category"}]}},{"id":"c_home_seckill","type":"seckill","visible":true,"props":{"title":"限时秒杀","subtitle":"每天 10 点开抢","goodsName":"当季赣南脐橙 5 斤装","image":"https://picsum.photos/seed/seckill-1/300/300","price":19.9,"originPrice":29.9,"endTime":"2026-12-31 22:00:00","bgColor":"#fff1f0"}},{"id":"c_home_title","type":"title","visible":true,"props":{"text":"为你推荐","subtitle":"猜你喜欢","align":"left","size":16,"color":"#333333","bold":true,"more":true}},{"id":"c_home_goods","type":"goods","visible":true,"props":{"layout":"grid","columns":2,"count":4,"showName":true,"showPrice":true,"showSales":true,"goods":[{"id":1,"name":"当季赣南脐橙 5 斤装","image":"https://picsum.photos/seed/goods-1/400/400","price":29.9,"sales":1280},{"id":2,"name":"智利车厘子 JJ 级 2 斤","image":"https://picsum.photos/seed/goods-2/400/400","price":89.0,"sales":642},{"id":3,"name":"每日坚果混合装 30 包","image":"https://picsum.photos/seed/goods-3/400/400","price":59.9,"sales":2310},{"id":4,"name":"低温鲜牛乳 950ml x 6","image":"https://picsum.photos/seed/goods-4/400/400","price":69.0,"sales":876}]}},{"id":"c_home_coupon","type":"coupon","visible":true,"props":{"title":"领券中心","bgColor":"#ffffff","coupons":[{"name":"新人专享 10 元券","amount":10,"threshold":0,"validDays":7},{"name":"满 99 减 15","amount":15,"threshold":99,"validDays":30},{"name":"生鲜 9 折券","amount":0,"discount":90,"threshold":50,"validDays":15}]}},{"id":"c_home_shop","type":"shopInfo","visible":true,"props":{"name":"易商城旗舰店","logo":"https://picsum.photos/seed/shop-logo/120/120","desc":"生鲜果蔬 · 次日达","phone":"400-800-1234","bgColor":"#ffffff"}}]}',
        '2026-10-26 10:00:00', '演示首页'),
       (2, '双十一活动页', 3, 0, 0, 'DEMO1111',
        '{"page":{"title":"双十一活动页","bgColor":"#fff5f5","bgImage":"","padding":0},"components":[{"id":"c_1111_banner","type":"banner","visible":true,"props":{"height":170,"radius":0,"interval":3,"margin":0,"images":[{"url":"https://picsum.photos/seed/1111-banner/750/340","link":"/goods/1"}]}},{"id":"c_1111_title","type":"title","visible":true,"props":{"text":"爆款直降","subtitle":"11.11 零点开抢","align":"center","size":18,"color":"#e02020","bold":true,"more":false}},{"id":"c_1111_goods","type":"goods","visible":true,"props":{"layout":"grid","columns":2,"count":4,"showName":true,"showPrice":true,"showSales":false,"goods":[{"id":1,"name":"当季赣南脐橙 5 斤装","image":"https://picsum.photos/seed/goods-1/400/400","price":19.9,"sales":0},{"id":3,"name":"每日坚果混合装 30 包","image":"https://picsum.photos/seed/goods-3/400/400","price":39.9,"sales":0}]}}]}',
        NULL, NULL, '未发布的草稿示例');
