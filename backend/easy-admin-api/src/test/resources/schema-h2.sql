-- 仅用于单元测试的 H2 建表脚本（MODE=MySQL）
DROP TABLE IF EXISTS m_member;
CREATE TABLE m_member
(
    id         bigint AUTO_INCREMENT PRIMARY KEY,
    openid     varchar(64) NOT NULL,
    unionid    varchar(64),
    nickname   varchar(64),
    avatar     varchar(255),
    phone      varchar(20),
    gender     tinyint     NOT NULL DEFAULT 0,
    status     tinyint     NOT NULL DEFAULT 1,
    created_at datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint      ,
    updated_by bigint      ,
    version    int          NOT NULL DEFAULT 0,
    deleted    tinyint     NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS mall_category;
CREATE TABLE mall_category
(
    id         bigint AUTO_INCREMENT PRIMARY KEY,
    name       varchar(32) NOT NULL,
    parent_id  bigint      NOT NULL DEFAULT 0,
    icon       varchar(255),
    sort       int         NOT NULL DEFAULT 0,
    status     tinyint     NOT NULL DEFAULT 1,
    created_at datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint      ,
    updated_by bigint      ,
    version    int          NOT NULL DEFAULT 0,
    deleted    tinyint     NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS mall_goods;
CREATE TABLE mall_goods
(
    id             bigint AUTO_INCREMENT PRIMARY KEY,
    category_id    bigint         NOT NULL,
    name           varchar(100)   NOT NULL,
    subtitle       varchar(200),
    main_image     varchar(255)   NOT NULL,
    images         varchar(2000),
    price          decimal(10, 2) NOT NULL,
    original_price decimal(10, 2),
    stock          int            NOT NULL DEFAULT 0,
    sales          int            NOT NULL DEFAULT 0,
    unit           varchar(16)             DEFAULT '件',
    detail         clob,
    status         tinyint        NOT NULL DEFAULT 1,
    sort           int            NOT NULL DEFAULT 0,
    created_at     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     bigint      ,
    updated_by     bigint      ,
    version        int          NOT NULL DEFAULT 0,
    deleted        tinyint        NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS cart_item;
CREATE TABLE cart_item
(
    id         bigint AUTO_INCREMENT PRIMARY KEY,
    user_id    bigint   NOT NULL,
    goods_id   bigint   NOT NULL,
    quantity   int      NOT NULL DEFAULT 1,
    checked    tinyint  NOT NULL DEFAULT 1,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    tinyint  NOT NULL DEFAULT 0
);

INSERT INTO mall_category (id, name, parent_id, sort, status)
VALUES (1, '新鲜水果', 0, 1, 1);

INSERT INTO mall_goods (category_id, name, main_image, price, stock, sales, status, sort)
VALUES (1, '当季赣南脐橙 5 斤装', 'https://picsum.photos/seed/orange/600/600', 29.90, 200, 12, 1, 1),
       (1, '海南贵妃芒 3 斤装', 'https://picsum.photos/seed/mango/600/600', 35.00, 150, 8, 1, 2);

DROP TABLE IF EXISTS mk_coupon;
CREATE TABLE mk_coupon
(
    id          bigint AUTO_INCREMENT PRIMARY KEY,
    coupon_code varchar(32)   NOT NULL,
    name        varchar(64)   NOT NULL,
    type        tinyint       NOT NULL DEFAULT 1,
    amount      decimal(10,2),
    discount    decimal(10,2),
    threshold   decimal(10,2) NOT NULL DEFAULT 0,
    total_count int           NOT NULL DEFAULT 0,
    send_count  int           NOT NULL DEFAULT 0,
    used_count  int           NOT NULL DEFAULT 0,
    per_limit   int           NOT NULL DEFAULT 1,
    valid_type  tinyint       NOT NULL DEFAULT 1,
    valid_start date,
    valid_end   date,
    valid_days  int,
    status      tinyint       NOT NULL DEFAULT 1,
    remark      varchar(255),
    created_at  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  bigint,
    updated_by  bigint,
    version     int           NOT NULL DEFAULT 0,
    deleted     tinyint       NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS mk_coupon_user;
CREATE TABLE mk_coupon_user
(
    id          bigint AUTO_INCREMENT PRIMARY KEY,
    coupon_id   bigint   NOT NULL,
    user_id     bigint   NOT NULL,
    status      tinyint  NOT NULL DEFAULT 0,
    order_no    varchar(32),
    received_at datetime,
    used_at     datetime,
    expired_at  datetime
);

-- 并发领券测试用：领取后 7 天有效的无门槛券，每人限领 1 张
INSERT INTO mk_coupon (id, coupon_code, name, type, amount, threshold, total_count, send_count, per_limit, valid_type, valid_days, status)
VALUES (1, 'TEST-NEWBIE-5', '新人立减券', 3, 5.00, 0, 100, 0, 1, 2, 7, 1);
