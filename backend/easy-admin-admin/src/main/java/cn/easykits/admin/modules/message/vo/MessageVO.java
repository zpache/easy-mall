package cn.easykits.admin.modules.message.vo;

import cn.easykits.admin.entity.Message;

import java.time.LocalDateTime;

/**
 * 消息中心列表项。已读是全局状态，直接取消息行上的 is_read。
 */
public record MessageVO(
        Long id,
        String type,
        String level,
        String title,
        String content,
        String bizType,
        Long bizId,
        String link,
        boolean read,
        Long readBy,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {

    public static MessageVO of(Message message) {
        return new MessageVO(
                message.getId(),
                message.getType(),
                message.getLevel(),
                message.getTitle(),
                message.getContent(),
                message.getBizType(),
                message.getBizId(),
                message.getLink(),
                Integer.valueOf(1).equals(message.getIsRead()),
                message.getReadBy(),
                message.getReadAt(),
                message.getCreatedAt()
        );
    }
}
