package com.easymall.admin.modules.file.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.file.dto.FileCompleteForm;
import com.easymall.admin.modules.file.service.FileStorageService;
import com.easymall.admin.modules.file.service.UploadService;
import com.easymall.admin.modules.file.vo.ImageUploadVO;
import com.easymall.admin.modules.file.vo.UploadCredentialVO;
import com.easymall.admin.modules.file.vo.UploadedFileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传。
 * <p>
 * 默认走 COS 直传：前端先取临时密钥，直传后回传 objectKey；
 * 本地模式（easy-mall.upload.mode=local）则走服务端转存。
 */
@Tag(name = "文件-上传")
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final UploadService uploadService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "获取上传凭证（COS 临时密钥 / 本地模式标识）")
    @PostMapping("/credential")
    public Result<UploadCredentialVO> credential() {
        return Result.ok(uploadService.credential());
    }

    @Operation(summary = "回传直传结果，登记 objectKey")
    @PostMapping("/complete")
    public Result<UploadedFileVO> complete(@Valid @RequestBody FileCompleteForm form) {
        return Result.ok(uploadService.complete(form));
    }

    @Operation(summary = "上传图片（仅本地存储模式）")
    @PostMapping("/image")
    public Result<ImageUploadVO> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileStorageService.storeImage(file));
    }
}
