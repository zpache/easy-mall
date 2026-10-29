package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 上传文件记录。
 * <p>
 * 前端直传 COS 成功后回传 objectKey，服务端校验并落此表，用于素材追溯与后续素材库复用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** COS 对象键，如 decorate/2026/10/xxx.png */
    private String objectKey;

    /** 素材名称，默认取原文件名，可重命名 */
    private String name;

    private String bucket;

    private String region;

    /** 可访问地址（默认桶域名或自定义 CDN 域名） */
    private String url;

    /** 字节数 */
    private Long size;

    private String mime;

    /** 使用场景，如 decorate */
    private String scene;
}
