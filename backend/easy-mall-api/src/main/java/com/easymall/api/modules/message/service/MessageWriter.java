package com.easymall.api.modules.message.service;

import com.easymall.api.entity.Message;
import com.easymall.api.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * 消息写入器：小程序侧的业务事件（支付成功、申请售后、提交评价）往管理端消息表落一条记录。
 * <p>
 * 调用方要能容忍失败：消息只是提醒，不能因为写消息失败就让下单/退款/评价回滚，
 * 所以 publish 内部把重复键异常吞掉，其余异常由调用方 try-catch 兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWriter {

    private final MessageMapper messageMapper;

    public void publish(String type, String level, String title, String content,
                        String bizType, Long bizId, String link, String dedupKey) {
        Message message = new Message();
        message.setType(type);
        message.setLevel(level == null ? Message.LEVEL_INFO : level);
        message.setTitle(title);
        message.setContent(content);
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setLink(link);
        message.setDedupKey(dedupKey);
        try {
            messageMapper.insert(message);
        } catch (DuplicateKeyException e) {
            // 同一事件已经推送过（支付回调重放等），忽略
            log.debug("消息已存在，跳过：{}", dedupKey);
        }
    }
}
