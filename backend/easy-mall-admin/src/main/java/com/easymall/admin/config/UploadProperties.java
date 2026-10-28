package com.easymall.admin.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地上传目录配置。
 * 文件落在 easy-mall.upload.dir（默认 ~/easy-mall/uploads），
 * 对外访问前缀为 easy-mall.upload.url-prefix（默认 /admin/uploads，已含 context-path）。
 */
@Getter
@Component
public class UploadProperties {

    @Value("${easy-mall.upload.dir:${user.home}/easy-mall/uploads}")
    private String dir;

    @Value("${easy-mall.upload.url-prefix:/admin/uploads}")
    private String urlPrefix;

    /**
     * 上传根目录的绝对规范路径，用于静态资源映射与写入。
     */
    public Path root() {
        return Paths.get(dir).toAbsolutePath().normalize();
    }
}
