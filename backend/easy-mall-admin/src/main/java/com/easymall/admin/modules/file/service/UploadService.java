package com.easymall.admin.modules.file.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.config.UploadProperties;
import com.easymall.admin.entity.SysFile;
import com.easymall.admin.mapper.SysFileMapper;
import com.easymall.admin.modules.file.dto.FileCompleteForm;
import com.easymall.admin.modules.file.vo.UploadCredentialVO;
import com.easymall.admin.modules.file.vo.UploadedFileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 上传编排：签发直传凭证 / 校验并登记前端回传的 objectKey。
 * <p>
 * 前端只负责传输，能否写、写到哪里由服务端策略决定；回执再次校验前缀与格式，
 * 避免前端伪造任意对象键登记到库里。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    /** 对象键允许的字符集，杜绝路径穿越与特殊字符 */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._/-]+$");

    private final UploadProperties uploadProperties;
    private final CosStsService cosStsService;
    private final SysFileMapper sysFileMapper;

    /**
     * 获取上传凭证：COS 模式签发 STS 临时密钥，本地模式让前端回退到服务端转存。
     */
    public UploadCredentialVO credential() {
        if (!uploadProperties.isCosMode()) {
            return UploadCredentialVO.local(uploadProperties.getMaxSize(), CosStsService.ALLOWED_EXTENSIONS);
        }
        return cosStsService.issue();
    }

    /**
     * 前端直传完成后回传 objectKey：校验 + 登记，重复回传按幂等处理。
     */
    public UploadedFileVO complete(FileCompleteForm form) {
        if (!uploadProperties.isCosMode()) {
            throw BusinessException.of("当前为本地存储模式，请使用 /upload/image 上传");
        }

        String objectKey = validateObjectKey(form.objectKey());
        long size = form.size() == null ? 0L : form.size();
        if (size > uploadProperties.getMaxSize()) {
            throw BusinessException.of("图片不能超过 %dMB".formatted(uploadProperties.getMaxSize() / 1024 / 1024));
        }

        SysFile existing = sysFileMapper.selectOne(
                Wrappers.<SysFile>lambdaQuery()
                        .eq(SysFile::getObjectKey, objectKey)
                        .last("LIMIT 1"));
        if (existing != null) {
            return toVO(existing);
        }

        SysFile record = new SysFile();
        record.setObjectKey(objectKey);
        record.setBucket(uploadProperties.getBucket());
        record.setRegion(uploadProperties.getRegion());
        record.setUrl(uploadProperties.objectUrl(objectKey));
        record.setSize(size);
        record.setMime(form.mime());
        record.setScene(form.scene());
        sysFileMapper.insert(record);

        log.info("登记直传文件: key={}, size={}, scene={}", objectKey, size, form.scene());
        return toVO(record);
    }

    /**
     * 校验对象键：必须落在授权前缀下、字符集合法、扩展名在白名单内。
     */
    private String validateObjectKey(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BusinessException.of("objectKey 不能为空");
        }
        String key = raw.trim();
        if (key.length() > 300) {
            throw BusinessException.of("objectKey 过长");
        }
        if (key.startsWith("/") || key.contains("..") || key.contains("//")) {
            throw BusinessException.of("非法的文件路径");
        }
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw BusinessException.of("非法的文件路径");
        }
        if (!key.startsWith(uploadProperties.normalizedKeyPrefix())) {
            throw BusinessException.of("文件未落在允许的上传目录下");
        }

        String ext = key.substring(key.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (key.lastIndexOf('.') < 0 || !CosStsService.ALLOWED_EXTENSIONS.contains(ext)) {
            throw BusinessException.of("仅支持 jpg / png / gif / webp / bmp 格式的图片");
        }
        return key;
    }

    private UploadedFileVO toVO(SysFile file) {
        return new UploadedFileVO(file.getId(), file.getObjectKey(), file.getUrl(), file.getSize(), file.getMime());
    }
}
