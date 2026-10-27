-- 仅用于单元测试的 H2 建表脚本（MODE=MySQL）
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user
(
    id            bigint AUTO_INCREMENT PRIMARY KEY,
    username      varchar(32)  NOT NULL,
    password      varchar(100) NOT NULL,
    nickname      varchar(32)  NOT NULL DEFAULT '',
    avatar        varchar(255),
    phone         varchar(20),
    email         varchar(100),
    dept_id       bigint,
    role          varchar(20)  NOT NULL DEFAULT 'OPERATOR',
    status        tinyint      NOT NULL DEFAULT 1,
    last_login_at datetime,
    created_at    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    bigint      ,
    updated_by    bigint      ,
    version       int          NOT NULL DEFAULT 0,
    deleted       tinyint      NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept
(
    id         bigint AUTO_INCREMENT PRIMARY KEY,
    pid        bigint      NOT NULL DEFAULT 0,
    name       varchar(50) NOT NULL,
    sort       int         NOT NULL DEFAULT 0,
    status     tinyint     NOT NULL DEFAULT 1,
    remark     varchar(255),
    created_at datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    version    int         NOT NULL DEFAULT 0,
    deleted    tinyint     NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role
(
    id         bigint AUTO_INCREMENT PRIMARY KEY,
    name       varchar(50) NOT NULL,
    code       varchar(50) NOT NULL,
    sort       int         NOT NULL DEFAULT 0,
    status     tinyint     NOT NULL DEFAULT 1,
    remark     varchar(255),
    created_at datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    version    int         NOT NULL DEFAULT 0,
    deleted    tinyint     NOT NULL DEFAULT 0,
    CONSTRAINT uk_sys_role_code UNIQUE (code)
);

DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu
(
    id                    bigint AUTO_INCREMENT PRIMARY KEY,
    pid                   bigint      NOT NULL DEFAULT 0,
    name                  varchar(50) NOT NULL,
    title                 varchar(50) NOT NULL,
    type                  varchar(20) NOT NULL DEFAULT 'menu',
    auth_code             varchar(100),
    path                  varchar(200),
    component             varchar(200),
    icon                  varchar(100),
    order_no              int         NOT NULL DEFAULT 0,
    status                tinyint     NOT NULL DEFAULT 1,
    badge                 varchar(50),
    badge_type            varchar(20),
    badge_variants        varchar(20),
    keep_alive            tinyint     NOT NULL DEFAULT 0,
    affix_tab             tinyint     NOT NULL DEFAULT 0,
    hide_in_menu          tinyint     NOT NULL DEFAULT 0,
    hide_children_in_menu tinyint     NOT NULL DEFAULT 0,
    hide_in_breadcrumb    tinyint     NOT NULL DEFAULT 0,
    hide_in_tab           tinyint     NOT NULL DEFAULT 0,
    link                  varchar(255),
    iframe_src            varchar(255),
    created_at            datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            bigint,
    updated_by            bigint,
    version               int         NOT NULL DEFAULT 0,
    deleted               tinyint     NOT NULL DEFAULT 0,
    CONSTRAINT uk_sys_menu_name UNIQUE (name)
);

DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role
(
    id         bigint AUTO_INCREMENT PRIMARY KEY,
    user_id    bigint   NOT NULL,
    role_id    bigint   NOT NULL,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    version    int      NOT NULL DEFAULT 0,
    deleted    tinyint  NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu
(
    id         bigint AUTO_INCREMENT PRIMARY KEY,
    role_id    bigint   NOT NULL,
    menu_id    bigint   NOT NULL,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    version    int      NOT NULL DEFAULT 0,
    deleted    tinyint  NOT NULL DEFAULT 0
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
    created_by bigint,
    updated_by bigint,
    version    int         NOT NULL DEFAULT 0,
    deleted    tinyint     NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS mall_brand;
CREATE TABLE mall_brand
(
    id         bigint AUTO_INCREMENT PRIMARY KEY,
    name       varchar(50) NOT NULL,
    logo       varchar(255),
    sort       int         NOT NULL DEFAULT 0,
    status     tinyint     NOT NULL DEFAULT 1,
    remark     varchar(255),
    created_at datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    version    int         NOT NULL DEFAULT 0,
    deleted    tinyint     NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS mall_goods;
CREATE TABLE mall_goods
(
    id             bigint AUTO_INCREMENT PRIMARY KEY,
    category_id    bigint         NOT NULL,
    brand_id       bigint,
    name           varchar(100)   NOT NULL,
    subtitle       varchar(200),
    main_image     varchar(255),
    images         varchar(2000),
    price          decimal(10, 2) NOT NULL DEFAULT 0,
    original_price decimal(10, 2),
    stock          int            NOT NULL DEFAULT 0,
    stock_warn     int            NOT NULL DEFAULT 10,
    sales          int            NOT NULL DEFAULT 0,
    unit           varchar(16),
    detail         text,
    status         tinyint        NOT NULL DEFAULT 1,
    sort           int            NOT NULL DEFAULT 0,
    created_at     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     bigint,
    updated_by     bigint,
    version        int            NOT NULL DEFAULT 0,
    deleted        tinyint        NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS mall_stock_log;
CREATE TABLE mall_stock_log
(
    id           bigint AUTO_INCREMENT PRIMARY KEY,
    goods_id     bigint      NOT NULL,
    type         varchar(16) NOT NULL,
    change_qty   int         NOT NULL,
    before_stock int         NOT NULL,
    after_stock  int         NOT NULL,
    remark       varchar(255),
    created_at   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   bigint
);

INSERT INTO mall_brand (id, name, sort, status)
VALUES (1, '果然鲜', 1, 1);

DROP TABLE IF EXISTS o_order;
CREATE TABLE o_order
(
    id               bigint AUTO_INCREMENT PRIMARY KEY,
    order_no         varchar(32)    NOT NULL,
    user_id          bigint         NOT NULL,
    total_amount     decimal(10, 2) NOT NULL,
    freight_amount   decimal(10, 2) NOT NULL DEFAULT 0,
    pay_amount       decimal(10, 2) NOT NULL,
    status           tinyint        NOT NULL DEFAULT 0,
    refund_status    tinyint        NOT NULL DEFAULT 0,
    receiver_name    varchar(32)    NOT NULL,
    receiver_phone   varchar(20)    NOT NULL,
    receiver_address varchar(255)   NOT NULL,
    delivery_company varchar(50),
    delivery_no      varchar(64),
    remark           varchar(255),
    pay_time         datetime,
    delivery_time    datetime,
    finish_time      datetime,
    cancel_time      datetime,
    cancel_reason    varchar(255),
    created_at       datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       bigint,
    updated_by       bigint,
    version          int            NOT NULL DEFAULT 0,
    deleted          tinyint        NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS o_order_item;
CREATE TABLE o_order_item
(
    id           bigint AUTO_INCREMENT PRIMARY KEY,
    order_id     bigint         NOT NULL,
    goods_id     bigint         NOT NULL,
    goods_name   varchar(100)   NOT NULL,
    goods_image  varchar(255),
    price        decimal(10, 2) NOT NULL,
    quantity     int            NOT NULL,
    total_amount decimal(10, 2) NOT NULL,
    created_at   datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   bigint,
    updated_by   bigint,
    version      int            NOT NULL DEFAULT 0,
    deleted      tinyint        NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS o_order_refund;
CREATE TABLE o_order_refund
(
    id           bigint AUTO_INCREMENT PRIMARY KEY,
    refund_no    varchar(32)    NOT NULL,
    order_id     bigint         NOT NULL,
    order_no     varchar(32)    NOT NULL,
    user_id      bigint         NOT NULL,
    amount       decimal(10, 2) NOT NULL,
    type         varchar(16)    NOT NULL DEFAULT 'refund',
    reason       varchar(255),
    status       tinyint        NOT NULL DEFAULT 0,
    audit_remark varchar(255),
    applied_at   datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at   datetime,
    handled_by   bigint,
    created_at   datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   bigint,
    updated_by   bigint,
    version      int            NOT NULL DEFAULT 0,
    deleted      tinyint        NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS o_order_comment;
CREATE TABLE o_order_comment
(
    id            bigint AUTO_INCREMENT PRIMARY KEY,
    order_id      bigint       NOT NULL,
    order_no      varchar(32)  NOT NULL,
    goods_id      bigint       NOT NULL,
    goods_name    varchar(100) NOT NULL,
    goods_image   varchar(255),
    user_id       bigint       NOT NULL,
    user_nickname varchar(64),
    score         tinyint      NOT NULL DEFAULT 5,
    content       varchar(500),
    images        varchar(1000),
    reply         varchar(500),
    replied_at    datetime,
    is_show       tinyint      NOT NULL DEFAULT 1,
    commented_at  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    bigint,
    updated_by    bigint,
    version       int          NOT NULL DEFAULT 0,
    deleted       tinyint      NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS m_member;
CREATE TABLE m_member
(
    id          bigint AUTO_INCREMENT PRIMARY KEY,
    openid      varchar(64)   NOT NULL,
    unionid     varchar(64),
    nickname    varchar(64),
    avatar      varchar(255),
    phone       varchar(20),
    gender      tinyint       NOT NULL DEFAULT 0,
    level_id    bigint        NOT NULL DEFAULT 1,
    points      int           NOT NULL DEFAULT 0,
    source      tinyint       NOT NULL DEFAULT 1,
    total_spend decimal(10,2) NOT NULL DEFAULT 0,
    order_count int           NOT NULL DEFAULT 0,
    status      tinyint       NOT NULL DEFAULT 1,
    created_at  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  bigint,
    updated_by  bigint,
    version     int           NOT NULL DEFAULT 0,
    deleted     tinyint       NOT NULL DEFAULT 0
);

INSERT INTO m_member (id, openid, nickname, phone, status, level_id, points, source)
VALUES (1, 'test-openid-1', '微信用户', '13800000001', 1, 2, 300, 1),
       (2, 'test-openid-2', '测试二号', '13900000002', 1, 1, 50, 0);

CREATE TABLE m_member_level
(
    id          bigint AUTO_INCREMENT PRIMARY KEY,
    name        varchar(50)   NOT NULL,
    discount    decimal(4, 1) NOT NULL DEFAULT 100.0,
    threshold   int           NOT NULL DEFAULT 0,
    description varchar(500),
    sort        int           NOT NULL DEFAULT 0,
    status      tinyint       NOT NULL DEFAULT 1,
    created_at  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  bigint,
    updated_by  bigint,
    version     int           NOT NULL DEFAULT 0,
    deleted     tinyint       NOT NULL DEFAULT 0
);

INSERT INTO m_member_level (id, name, discount, threshold, description, sort, status)
VALUES (1, '普通会员', 100.0, 0, '注册即享', 1, 1),
       (2, '银卡会员', 98.0, 500, '全场 98 折', 2, 1);

CREATE TABLE m_points_log
(
    id         bigint AUTO_INCREMENT PRIMARY KEY,
    user_id    bigint      NOT NULL,
    change     int         NOT NULL,
    balance    int         NOT NULL,
    type       varchar(16) NOT NULL DEFAULT 'earn',
    order_no   varchar(32),
    remark     varchar(255),
    created_at datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint
);

INSERT INTO m_points_log (id, user_id, change, balance, type, remark)
VALUES (5001, 1, 300, 300, 'earn', '历史积分迁移');

CREATE TABLE m_member_tag
(
    id         bigint AUTO_INCREMENT PRIMARY KEY,
    name       varchar(50) NOT NULL,
    color      varchar(20),
    remark     varchar(255),
    created_at datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    version    int         NOT NULL DEFAULT 0,
    deleted    tinyint     NOT NULL DEFAULT 0
);

INSERT INTO m_member_tag (id, name, color, remark)
VALUES (1, '高价值用户', 'red', '月消费高'),
       (2, '新客', 'blue', '注册 30 天内');

CREATE TABLE m_member_user_tag
(
    id      bigint AUTO_INCREMENT PRIMARY KEY,
    user_id bigint NOT NULL,
    tag_id  bigint NOT NULL,
    CONSTRAINT uk_user_tag UNIQUE (user_id, tag_id)
);

INSERT INTO m_member_user_tag (id, user_id, tag_id)
VALUES (6001, 1, 1),
       (6002, 2, 2);

INSERT INTO o_order (id, order_no, user_id, total_amount, freight_amount, pay_amount, status, refund_status,
                        receiver_name, receiver_phone, receiver_address, pay_time)
VALUES (1002, 'EM20261021000002', 1, 29.90, 0.00, 29.90, 1, 0, '周予安', '13800000002', '杭州市西湖区文三路 199 号',
        '2026-10-21 09:30:00'),
       (1007, 'EM20261024000007', 1, 59.90, 0.00, 59.90, 2, 1, '林小满', '13800000001', '上海市浦东新区张江路 88 号',
        '2026-10-24 13:15:00');

INSERT INTO o_order_item (id, order_id, goods_id, goods_name, price, quantity, total_amount)
VALUES (2002, 1002, 1, '当季赣南脐橙 5 斤装', 29.90, 1, 29.90),
       (2008, 1007, 3, '每日坚果混合装 30 包', 59.90, 1, 59.90);

INSERT INTO o_order_refund (id, refund_no, order_id, order_no, user_id, amount, type, reason, status, applied_at)
VALUES (3001, 'RF20261024000001', 1007, 'EM20261024000007', 1, 59.90, 'return', '坚果受潮，申请退货退款', 0,
        '2026-10-25 09:10:00');

INSERT INTO o_order_comment (id, order_id, order_no, goods_id, goods_name, user_id, user_nickname, score, content,
                                is_show, commented_at)
VALUES (4003, 1007, 'EM20261024000007', 3, '每日坚果混合装 30 包', 1, '微信用户', 2, '有两个已经软烂了', 0,
        '2026-10-24 10:30:00');

INSERT INTO mall_goods (id, category_id, brand_id, name, price, stock, stock_warn, status)
VALUES (1, 1, 1, '当季赣南脐橙 5 斤装', 29.90, 20, 10, 1);

INSERT INTO sys_user (id, username, password, nickname, role, status, dept_id)
VALUES (1, 'admin', '$2a$10$p4026qPjmgn80u.iSAh7BOwesH4y6Vc2SsT8eTwpPOLbS0Sg9RWKi', '超级管理员', 'ADMIN', 1, 1);

INSERT INTO sys_dept (id, pid, name, sort, status)
VALUES (1, 0, '易商城总部', 1, 1);

INSERT INTO sys_role (id, name, code, sort, status)
VALUES (1, '超级管理员', 'ADMIN', 1, 1);

INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1);

INSERT INTO sys_menu (id, pid, name, title, type, auth_code, path, icon, order_no, status)
VALUES (1, 0, 'System', '系统管理', 'catalog', NULL, '/system', 'lucide:settings', 90, 1),
       (2, 1, 'SystemUser', '用户管理', 'menu', 'system:user:list', '/system/user', 'lucide:users', 1, 1);

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES (1, 1),
       (1, 2);

INSERT INTO mall_category (id, name, parent_id, sort, status)
VALUES (1, '新鲜水果', 0, 1, 1);
