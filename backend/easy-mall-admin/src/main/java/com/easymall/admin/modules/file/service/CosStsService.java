package com.easymall.admin.modules.file.service;

import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.config.UploadProperties;
import com.easymall.admin.modules.file.vo.UploadCredentialVO;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sts.v20180813.StsClient;
import com.tencentcloudapi.sts.v20180813.models.Credentials;
import com.tencentcloudapi.sts.v20180813.models.GetFederationTokenRequest;
import com.tencentcloudapi.sts.v20180813.models.GetFederationTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 签发 COS 直传所需的临时密钥（GetFederationToken）。
 * <p>
 * 策略收口在服务端：只允许向配置的前缀下写对象，前端拿到密钥也无法读取或删除其它数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CosStsService {

    /** 允许的图片扩展名（与本地模式保持一致） */
    public static final List<String> ALLOWED_EXTENSIONS =
            List.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    /**
     * 直传需要的动作：普通上传 + 分片上传（大图走分片）+ 分片收尾。
     */
    private static final String POLICY_TEMPLATE = """
            {"version":"2.0","statement":[{"effect":"allow","action":[
            "cos:PutObject","cos:PostObject","cos:InitiateMultipartUpload",
            "cos:UploadPart","cos:CompleteMultipartUpload","cos:AbortMultipartUpload","cos:ListParts"
            ],"resource":["%s"]}]}""";

    private final UploadProperties uploadProperties;

    /**
     * 签发临时密钥；未配置 COS 时抛出明确提示，避免前端拿到空密钥后静默失败。
     */
    public UploadCredentialVO issue() {
        if (!uploadProperties.isCosConfigured()) {
            throw BusinessException.of("COS 未配置：请在后端配置 secret-id / secret-key / bucket / region");
        }

        Credential credential = new Credential(uploadProperties.getSecretId(), uploadProperties.getSecretKey());

        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(uploadProperties.getStsEndpoint());
        httpProfile.setConnTimeout(5);
        httpProfile.setReadTimeout(10);

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        StsClient client = new StsClient(credential, uploadProperties.getRegion(), clientProfile);

        GetFederationTokenRequest request = new GetFederationTokenRequest();
        request.setName("easymall-admin-upload");
        request.setDurationSeconds(uploadProperties.getDurationSeconds());
        request.setPolicy(buildPolicy());

        try {
            GetFederationTokenResponse response = client.GetFederationToken(request);
            Credentials credentials = response.getCredentials();
            long expiredTime = response.getExpiredTime();
            long startTime = expiredTime - uploadProperties.getDurationSeconds();
            log.info("签发 COS 临时密钥成功: prefix={}, 有效期至={}", uploadProperties.normalizedKeyPrefix(), expiredTime);

            return new UploadCredentialVO(
                    "cos",
                    credentials.getTmpSecretId(),
                    credentials.getTmpSecretKey(),
                    credentials.getToken(),
                    startTime,
                    expiredTime,
                    uploadProperties.getBucket(),
                    uploadProperties.getRegion(),
                    uploadProperties.normalizedKeyPrefix(),
                    uploadProperties.getDomain(),
                    uploadProperties.getMaxSize(),
                    ALLOWED_EXTENSIONS);
        } catch (TencentCloudSDKException e) {
            log.error("签发 COS 临时密钥失败: {}", e.getMessage(), e);
            throw BusinessException.of("获取上传凭证失败，请检查 COS 配置与 STS 权限");
        }
    }

    /**
     * 资源粒度到「桶 + 前缀」，且只授予写动作。
     */
    private String buildPolicy() {
        String resource = "qcs::cos:%s:uid/%s:%s/%s*".formatted(
                uploadProperties.getRegion(),
                uploadProperties.appId(),
                uploadProperties.getBucket(),
                uploadProperties.normalizedKeyPrefix());
        return POLICY_TEMPLATE.formatted(resource);
    }
}
