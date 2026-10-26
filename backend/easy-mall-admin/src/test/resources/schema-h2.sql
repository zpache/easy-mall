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

INSERT INTO sys_user (username, password, nickname, role, status)
VALUES ('admin', '$2a$10$p4026qPjmgn80u.iSAh7BOwesH4y6Vc2SsT8eTwpPOLbS0Sg9RWKi', '超级管理员', 'ADMIN', 1);
