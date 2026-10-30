package com.easymall.api.modules.file.controller;

import com.easymall.api.common.api.Result;
import com.easymall.api.modules.file.service.FileStorageService;
import com.easymall.api.modules.file.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传（需登录）。上传结果直接写回用户资料的头像字段。
 *
 * <p>注意：上传接口走 /uploads/**（需登录），静态访问走 /files/**（免登录），
 * 两者分开是为了避免静态资源的白名单把上传接口一起放行。</p>
 */
@Tag(name = "文件")
@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传头像")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileVO> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return Result.ok(new FileVO(fileStorageService.storeAvatar(file)));
    }

    @Operation(summary = "上传评价晒图")
    @PostMapping(value = "/comment-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileVO> uploadCommentImage(@RequestPart("file") MultipartFile file) {
        return Result.ok(new FileVO(fileStorageService.storeCommentImage(file)));
    }
}
