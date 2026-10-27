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

DROP TABLE IF EXISTS mall_goods;
CREATE TABLE mall_goods
(
    id         bigint AUTO_INCREMENT PRIMARY KEY,
    category_id bigint,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    version    int      NOT NULL DEFAULT 0,
    deleted    tinyint  NOT NULL DEFAULT 0
);

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
