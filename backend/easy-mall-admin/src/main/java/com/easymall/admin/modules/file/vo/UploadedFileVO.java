package com.easymall.admin.modules.file.vo;

/**
 * 已登记的上传文件。
 *
 * @param id        记录 ID
 * @param objectKey COS 对象键
 * @param url       可访问地址
 * @param size      字节数
 * @param mime      内容类型
 */
public record UploadedFileVO(Long id, String objectKey, String url, Long size, String mime) {
}
