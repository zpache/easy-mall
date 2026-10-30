package com.easymall.api.modules.file.service;

import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.config.UploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地上传文件存储。
 *
 * <p>头像文件按 avatar/yyyy/MM/dd/uuid.ext 落盘，返回可直接访问的绝对地址。
 * 文件类型按魔数判断，不信任客户端文件名与 Content-Type。</p>
 */
@Slf4j
@Service
public class FileStorageService {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final int MAGIC_LENGTH = 12;

    private final Path root;
    private final long avatarMaxSize;
    private final long commentMaxSize;
    private final String publicBase;

    public FileStorageService(UploadProperties properties) {
        this.root = Paths.get(properties.getDir()).toAbsolutePath().normalize();
        this.avatarMaxSize = properties.getAvatarMaxSize();
        this.commentMaxSize = properties.getCommentMaxSize();
        this.publicBase = properties.getPublicBase();
    }

    /**
     * 保存头像并返回访问地址。
     */
    public String storeAvatar(MultipartFile file) {
        return store(file, "avatar", avatarMaxSize);
    }

    /**
     * 保存评价晒图并返回访问地址（comment/yyyy/MM/dd）。
     */
    public String storeCommentImage(MultipartFile file) {
        return store(file, "comment", commentMaxSize);
    }

    private String store(MultipartFile file, String dir, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_EMPTY);
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE);
        }
        byte[] head = readHead(file);
        String extension = detectImageExtension(head);
        if (extension == null) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }

        String relativePath = dir + "/" + LocalDate.now().format(DATE_DIR)
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            // 理论上不会发生，兜底防目录穿越
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("保存上传文件失败: {}", relativePath, e);
            throw new BusinessException(ResultCode.FILE_SAVE_FAILED);
        }
        return buildUrl(relativePath);
    }

    /**
     * 本地存储根目录，供静态资源映射使用。
     */
    public Path getRoot() {
        return root;
    }

    private String buildUrl(String relativePath) {
        String path = "/files/" + relativePath;
        if (StringUtils.hasText(publicBase)) {
            return publicBase.replaceAll("/+$", "") + path;
        }
        // 按客户端实际访问的协议 + host 拼装，真机用局域网 IP 访问时地址依然可用
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).toUriString();
    }

    private byte[] readHead(MultipartFile file) {
        byte[] head = new byte[MAGIC_LENGTH];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(head, 0, head.length);
            if (read < head.length) {
                byte[] actual = new byte[read];
                System.arraycopy(head, 0, actual, 0, read);
                return actual;
            }
            return head;
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BusinessException(ResultCode.FILE_SAVE_FAILED);
        }
    }

    /**
     * 仅接受 jpg / png / gif / webp，其余一律拒绝。
     */
    private String detectImageExtension(byte[] head) {
        if (head.length >= 3
                && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (head.length >= 8
                && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
                && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A) {
            return "png";
        }
        if (head.length >= 6 && startsWith(head, 0, "GIF8")) {
            return "gif";
        }
        if (head.length >= 12 && startsWith(head, 0, "RIFF") && startsWith(head, 8, "WEBP")) {
            return "webp";
        }
        return null;
    }

    private boolean startsWith(byte[] data, int offset, String ascii) {
        byte[] expected = ascii.getBytes(StandardCharsets.US_ASCII);
        if (data.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (data[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
