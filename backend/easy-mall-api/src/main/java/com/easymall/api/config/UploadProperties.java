package com.easymall.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地上传配置。头像等用户上传文件落在本地磁盘，由 /files/** 静态映射对外提供访问。
 */
@Data
@ConfigurationProperties(prefix = "easy-mall.upload")
public class UploadProperties {

    /** 存储根目录，相对路径以进程工作目录为基准（已 gitignore） */
    private String dir = "./uploads";

    /**
     * 对外访问前缀，例如 https://cdn.example.com。
     * 留空则按当前请求的协议 + host 自动拼装，真机调试时无需额外配置。
     */
    private String publicBase = "";

    /** 头像单文件大小上限（字节），默认 2MB */
    private long avatarMaxSize = 2 * 1024 * 1024L;

    /** 评价晒图单文件大小上限（字节），默认 5MB */
    private long commentMaxSize = 5 * 1024 * 1024L;
}
