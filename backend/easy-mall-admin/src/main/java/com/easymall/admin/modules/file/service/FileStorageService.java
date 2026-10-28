package com.easymall.admin.modules.file.service;

import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.config.UploadProperties;
import com.easymall.admin.modules.file.vo.ImageUploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 图片上传落盘。
 * 安全要点：不信任客户端文件名与 Content-Type，按文件头判定真实类型；
 * 文件名由服务端生成，杜绝路径穿越与可执行后缀。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final int HEAD_LENGTH = 12;
    private static final DateTimeFormatter MONTH_PATH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final UploadProperties uploadProperties;

    public ImageUploadVO storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.of("请选择要上传的图片");
        }
        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw BusinessException.of("图片不能超过 %dMB".formatted(uploadProperties.getMaxSize() / 1024 / 1024));
        }

        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(HEAD_LENGTH);
        } catch (IOException e) {
            throw BusinessException.of("图片读取失败，请重试");
        }

        String ext = detectImageType(head);
        if (ext == null) {
            throw BusinessException.of("仅支持 jpg / png / gif / webp / bmp 格式的图片");
        }

        LocalDate today = LocalDate.now();
        String monthPath = today.format(MONTH_PATH);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        Path targetDir = uploadProperties.root().resolve(monthPath).normalize();
        if (!targetDir.startsWith(uploadProperties.root())) {
            throw BusinessException.of("非法的存储路径");
        }

        try {
            Files.createDirectories(targetDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("保存上传图片失败: dir={}", targetDir, e);
            throw BusinessException.of("图片保存失败，请稍后重试");
        }

        String url = "%s/%s/%s".formatted(uploadProperties.normalizedUrlPrefix(), monthPath, fileName);
        return new ImageUploadVO(url, fileName, file.getSize());
    }

    /**
     * 按文件头识别图片类型，返回规范扩展名；非图片返回 null。
     */
    private String detectImageType(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A) {
            return "png";
        }
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return "gif";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        if (b.length >= 2 && b[0] == 'B' && b[1] == 'M') {
            return "bmp";
        }
        return null;
    }
}
