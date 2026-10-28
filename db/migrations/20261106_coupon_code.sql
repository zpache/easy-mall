-- ============================================================
-- 优惠券对外随机码
-- 背景：营销中心的券一直以自增主键作为对外标识，小程序端传入 id=1、id=2
--       即可遍历全部券，暴露上架节奏与券总量。改为后端生成的随机码，
--       自增主键只在管理端内部使用，不出网关。
-- 格式：C + yyMMdd + 10 位随机串（字符集去掉易混淆的 I/O/0/1），如 C261028K7P2M9XQ4AB
-- ============================================================

ALTER TABLE mk_coupon
    ADD COLUMN coupon_code varchar(32) NULL COMMENT '对外随机码（小程序端唯一标识，不暴露自增主键）' AFTER id;

-- 存量回填：按创建日期 + 随机串生成，保证与 Java 侧格式一致的前缀
UPDATE mk_coupon
SET coupon_code = CONCAT(
        'C',
        DATE_FORMAT(created_at, '%y%m%d'),
        UPPER(SUBSTRING(MD5(CONCAT_WS('-', id, RAND(), UUID())), 1, 10))
    )
WHERE coupon_code IS NULL;

ALTER TABLE mk_coupon
    MODIFY COLUMN coupon_code varchar(32) NOT NULL COMMENT '对外随机码（小程序端唯一标识，不暴露自增主键）';

ALTER TABLE mk_coupon
    ADD UNIQUE KEY uk_coupon_code (coupon_code);
