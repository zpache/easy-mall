package com.easymall.admin.modules.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 前端直传完成后的回执：校验并登记 objectKey。
 *
 * @param objectKey COS 对象键，必须落在授权前缀下
 * @param size      文件字节数
 * @param mime      内容类型，可选
 * @param scene     使用场景，可选（如 decorate）
 */
public record FileCompleteForm(
        @NotBlank(message = "objectKey 不能为空")
        @Size(max = 300, message = "objectKey 过长")
        String objectKey,

        Long size,

        @Size(max = 100, message = "mime 过长")
        String mime,

        @Size(max = 50, message = "scene 过长")
        String scene
) {
}
