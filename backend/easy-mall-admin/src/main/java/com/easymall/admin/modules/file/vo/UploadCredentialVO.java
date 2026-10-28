package com.easymall.admin.modules.file.vo;

import java.util.List;

/**
 * 前端直传凭证。
 * <p>
 * mode=cos 时携带 STS 临时密钥，前端据此直传 COS 并回传 objectKey；
 * mode=local 时前端改走服务端 /upload/image 转存。
 *
 * @param mode            上传模式：cos | local
 * @param tmpSecretId     临时 SecretId
 * @param tmpSecretKey    临时 SecretKey
 * @param sessionToken    临时会话令牌
 * @param startTime       凭证生效时间（秒级时间戳）
 * @param expiredTime     凭证过期时间（秒级时间戳）
 * @param bucket          存储桶
 * @param region          地域
 * @param keyPrefix       对象键前缀（授权范围）
 * @param domain          访问域名，空则用桶默认域名
 * @param maxSize         单文件字节上限
 * @param allowExtensions 允许的扩展名
 */
public record UploadCredentialVO(
        String mode,
        String tmpSecretId,
        String tmpSecretKey,
        String sessionToken,
        Long startTime,
        Long expiredTime,
        String bucket,
        String region,
        String keyPrefix,
        String domain,
        long maxSize,
        List<String> allowExtensions
) {

    public static UploadCredentialVO local(long maxSize, List<String> allowExtensions) {
        return new UploadCredentialVO("local", null, null, null, null, null,
                null, null, null, null, maxSize, allowExtensions);
    }
}
