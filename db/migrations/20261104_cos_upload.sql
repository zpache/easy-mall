-- ============================================================
-- 上传文件记录（COS 直传回执登记）
-- 执行：mysql -uroot -p easy_mall < db/migrations/20261104_cos_upload.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_file
(
    id         bigint AUTO_INCREMENT PRIMARY KEY,
    object_key varchar(300) NOT NULL COMMENT 'COS 对象键，如 decorate/2026/10/xxx.png',
    bucket     varchar(100) NOT NULL COMMENT '存储桶',
    region     varchar(50)  NOT NULL COMMENT '地域',
    url        varchar(500) NOT NULL COMMENT '可访问地址',
    size       bigint       NOT NULL DEFAULT 0 COMMENT '字节数',
    mime       varchar(100)          DEFAULT NULL COMMENT '内容类型',
    scene      varchar(50)           DEFAULT NULL COMMENT '使用场景，如 decorate',
    created_at datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint                DEFAULT NULL,
    updated_by bigint                DEFAULT NULL,
    version    int          NOT NULL DEFAULT 0,
    deleted    tinyint      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_file_object_key (object_key),
    KEY idx_file_scene (scene),
    KEY idx_file_created (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='上传文件记录';
