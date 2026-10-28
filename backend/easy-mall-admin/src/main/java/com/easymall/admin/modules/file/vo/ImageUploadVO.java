package com.easymall.admin.modules.file.vo;

/**
 * 图片上传结果。
 *
 * @param url  可直接使用的访问地址（含 context-path）
 * @param name 服务端生成的文件名
 * @param size 文件字节数
 */
public record ImageUploadVO(String url, String name, long size) {
}
