package com.easymall.admin.modules.file.vo;

import com.easymall.admin.entity.SysFile;

import java.time.LocalDateTime;

/**
 * 素材列表项。
 */
public record MaterialVO(
        Long id,
        String name,
        String url,
        String objectKey,
        Long size,
        String mime,
        String scene,
        LocalDateTime createdAt) {

    public static MaterialVO of(SysFile file) {
        return new MaterialVO(file.getId(), file.getName(), file.getUrl(), file.getObjectKey(),
                file.getSize(), file.getMime(), file.getScene(), file.getCreatedAt());
    }
}
