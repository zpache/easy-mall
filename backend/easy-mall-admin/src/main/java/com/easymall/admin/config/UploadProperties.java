package com.easymall.admin.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 上传配置。
 * <p>
 * mode=cos 时走前端直传 COS（STS 临时密钥）；mode=local 时走服务端转存本地磁盘（仅开发环境用）。
 */
@Getter
@Component
public class UploadProperties {

    /** cos | local */
    @Value("${easy-mall.upload.mode:cos}")
    private String mode;

    /** 本地上传目录（local 模式） */
    @Value("${easy-mall.upload.dir:${user.home}/easy-mall/uploads}")
    private String dir;

    /** 本地静态访问前缀（local 模式，须与实际 context-path 一致） */
    @Value("${easy-mall.upload.url-prefix:/admin/uploads}")
    private String urlPrefix;

    /** 单图大小上限，默认 5MB */
    @Value("${easy-mall.upload.max-size:5242880}")
    private long maxSize;

    /* ---------------- COS ---------------- */

    @Value("${easy-mall.upload.cos.secret-id:}")
    private String secretId;

    @Value("${easy-mall.upload.cos.secret-key:}")
    private String secretKey;

    /** 形如 easymall-1250000000，后缀即 APPID */
    @Value("${easy-mall.upload.cos.bucket:}")
    private String bucket;

    @Value("${easy-mall.upload.cos.region:ap-guangzhou}")
    private String region;

    /** 对象键前缀，同时作为临时密钥策略的授权范围 */
    @Value("${easy-mall.upload.cos.key-prefix:decorate/}")
    private String keyPrefix;

    /** 临时密钥有效期（秒），默认 30 分钟 */
    @Value("${easy-mall.upload.cos.duration-seconds:1800}")
    private long durationSeconds;

    /** 访问域名：留空则用桶默认域名，可填 CDN 加速域名（如 https://img.example.com） */
    @Value("${easy-mall.upload.cos.domain:}")
    private String domain;

    @Value("${easy-mall.upload.cos.sts-endpoint:sts.tencentcloudapi.com}")
    private String stsEndpoint;

    public boolean isCosMode() {
        return "cos".equalsIgnoreCase(mode);
    }

    /**
     * COS 关键参数是否齐备。
     */
    public boolean isCosConfigured() {
        return notBlank(secretId) && notBlank(secretKey) && notBlank(bucket) && notBlank(region);
    }

    /**
     * 从 bucket 名解析 APPID（bucket 形如 name-1250000000）。
     */
    public String appId() {
        if (!notBlank(bucket)) {
            return "";
        }
        int idx = bucket.lastIndexOf('-');
        return idx > 0 && idx < bucket.length() - 1 ? bucket.substring(idx + 1) : "";
    }

    /**
     * 规范化对象键前缀：确保以 / 结尾，便于直接拼接。
     */
    public String normalizedKeyPrefix() {
        String prefix = notBlank(keyPrefix) ? keyPrefix.trim() : "decorate/";
        if (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    /**
     * 由对象键拼出可公开访问的 URL。
     */
    public String objectUrl(String objectKey) {
        String base = notBlank(domain)
                ? domain.trim().replaceAll("/+$", "")
                : "https://" + bucket + ".cos." + region + ".myqcloud.com";
        return base + "/" + objectKey;
    }

    /**
     * 本地模式下的访问前缀（去掉末尾斜杠）。
     */
    public String normalizedUrlPrefix() {
        if (!notBlank(urlPrefix)) {
            return "/admin/uploads";
        }
        return urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
    }

    public java.nio.file.Path root() {
        return java.nio.file.Paths.get(dir).toAbsolutePath().normalize();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
