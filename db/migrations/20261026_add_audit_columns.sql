-- =============================================================
-- 增量脚本：已有库补齐审计字段与乐观锁版本号
-- 背景：BaseEntity 新增 created_by / updated_by / version，
--      已初始化过数据的库不需要重跑 easy_mall.sql（会清库），执行本脚本即可。
-- 用法：mysql -uroot -p easy_mall < db/migrations/20261026_add_audit_columns.sql
-- 注意：脚本不幂等，重复执行会报 Duplicate column name，属正常现象。
-- 说明：cart_item 不继承 BaseEntity（购物车为临时数据、物理删除），不加这三列。
-- =============================================================

ALTER TABLE `sys_user`
    ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人 ID' AFTER `updated_at`,
    ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '最后修改人 ID' AFTER `created_by`,
    ADD COLUMN `version`    int    NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `updated_by`;

ALTER TABLE `mall_category`
    ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人 ID' AFTER `updated_at`,
    ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '最后修改人 ID' AFTER `created_by`,
    ADD COLUMN `version`    int    NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `updated_by`;

ALTER TABLE `mall_goods`
    ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人 ID' AFTER `updated_at`,
    ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '最后修改人 ID' AFTER `created_by`,
    ADD COLUMN `version`    int    NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `updated_by`;

ALTER TABLE `app_user`
    ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人 ID' AFTER `updated_at`,
    ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '最后修改人 ID' AFTER `created_by`,
    ADD COLUMN `version`    int    NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `updated_by`;

ALTER TABLE `user_address`
    ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人 ID' AFTER `updated_at`,
    ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '最后修改人 ID' AFTER `created_by`,
    ADD COLUMN `version`    int    NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `updated_by`;

ALTER TABLE `mall_order`
    ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人 ID' AFTER `updated_at`,
    ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '最后修改人 ID' AFTER `created_by`,
    ADD COLUMN `version`    int    NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `updated_by`;

ALTER TABLE `order_item`
    ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人 ID' AFTER `updated_at`,
    ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT '最后修改人 ID' AFTER `created_by`,
    ADD COLUMN `version`    int    NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `updated_by`;
