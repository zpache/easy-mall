package com.easymall.admin.modules.file.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.file.service.FileStorageService;
import com.easymall.admin.modules.file.vo.ImageUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传（需登录，装修素材等场景使用）。
 */
@Tag(name = "文件-上传")
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传图片")
    @PostMapping("/image")
    public Result<ImageUploadVO> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileStorageService.storeImage(file));
    }
}
