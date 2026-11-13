package cn.easykits.admin.modules.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.easykits.admin.common.api.PageResult;
import cn.easykits.admin.common.api.ResultCode;
import cn.easykits.admin.common.exception.BusinessException;
import cn.easykits.admin.entity.Message;
import cn.easykits.admin.mapper.MessageMapper;
import cn.easykits.admin.modules.message.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消息中心：发布、列表、未读数、已读。
 * <p>
 * 已读是<b>全局</b>的：消息广播给整个运营团队，谁先确认就算处理完，
 * 其他人不用再看到同一条，因此未读数就是「is_read = 0 的条数」，
 * 与「谁登录」无关。read_by / read_at 只用于追溯是谁处理的。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;

    /**
     * 发布一条消息。dedupKey 重复时直接忽略（支付回调重放、定时任务重复扫描都会走到这里）。
     *
     * @return true 真正写入了；false 命中 dedup_key 被跳过
     */
    public boolean publish(String type, String level, String title, String content,
                           String bizType, Long bizId, String link, String dedupKey) {
        Message message = new Message();
        message.setType(type);
        message.setLevel(StringUtils.hasText(level) ? level : Message.LEVEL_INFO);
        message.setTitle(title);
        message.setContent(content);
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setLink(link);
        message.setDedupKey(dedupKey);
        message.setIsRead(0);
        try {
            messageMapper.insert(message);
            return true;
        } catch (DuplicateKeyException e) {
            // uk_message_dedup 兜底：同一事件已经推送过，不重复打扰
            log.debug("消息已存在，跳过：{}", dedupKey);
            return false;
        }
    }

    /**
     * 指定类型下「已经提醒过」的业务 ID 集合：since 之后产生的，或者至今仍未读的。
     * <p>
     * 定时任务用它做批量去重：一条未读的老提醒也算提醒过，
     * 否则 5 分钟一轮的扫描会把同一个商品反复写进铃铛。
     */
    public Set<Long> notifiedBizIds(String type, LocalDateTime since) {
        List<Message> list = messageMapper.selectList(Wrappers.<Message>lambdaQuery()
                .eq(Message::getType, type)
                .isNotNull(Message::getBizId)
                .and(w -> w.ge(Message::getCreatedAt, since).or().eq(Message::getIsRead, 0))
                .select(Message::getBizId));
        return list.stream().map(Message::getBizId).collect(Collectors.toSet());
    }

    /**
     * 自动消除：类型下所有未读、且业务 ID 不在 keepBizIds 里的消息置为已读。
     * <p>
     * read_by 留空表示「系统自动消除」，与管理员手动确认区分开。
     * 库存补足或商品下架后，旧的库存预警走这里清掉，铃铛里不会留假警报。
     *
     * @return 本次消除的条数
     */
    public int autoResolve(String type, Collection<Long> keepBizIds) {
        boolean keep = keepBizIds != null && !keepBizIds.isEmpty();
        return messageMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .eq(Message::getType, type)
                .eq(Message::getIsRead, 0)
                .notIn(keep, Message::getBizId, keepBizIds)
                .set(Message::getIsRead, 1)
                .set(Message::getReadAt, LocalDateTime.now()));
    }

    /**
     * 分页查询。
     *
     * @param onlyUnread true 只看未读（铃铛弹层用）
     */
    public PageResult<MessageVO> pageQuery(long pageNum, long pageSize, String type, Boolean onlyUnread) {
        LambdaQueryWrapper<Message> wrapper = Wrappers.<Message>lambdaQuery()
                .eq(StringUtils.hasText(type), Message::getType, type)
                .eq(Boolean.TRUE.equals(onlyUnread), Message::getIsRead, 0)
                .orderByDesc(Message::getId);

        Page<Message> page = messageMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<MessageVO> voList = page.getRecords().stream().map(MessageVO::of).toList();
        return new PageResult<>(voList, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /** 未读条数，铃铛角标用 */
    public long unreadCount() {
        return messageMapper.selectCount(Wrappers.<Message>lambdaQuery().eq(Message::getIsRead, 0));
    }

    /**
     * 单条已读：谁点的就记谁，已读后所有人都不再看到。
     * 重复点击是幂等的（条件更新只在 is_read = 0 时生效）。
     */
    public void markRead(Long userId, Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
        }
        if (Integer.valueOf(1).equals(message.getIsRead())) {
            return;
        }
        messageMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .eq(Message::getId, messageId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1)
                .set(Message::getReadBy, userId)
                .set(Message::getReadAt, LocalDateTime.now()));
    }

    /** 全部已读，返回本次置为已读的条数 */
    public int markAllRead(Long userId) {
        return messageMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1)
                .set(Message::getReadBy, userId)
                .set(Message::getReadAt, LocalDateTime.now()));
    }

    /** 清理过期消息（默认保留 180 天），避免表无限增长 */
    public int cleanExpired(int keepDays) {
        LocalDateTime before = LocalDateTime.now().minusDays(keepDays);
        List<Message> expired = messageMapper.selectList(Wrappers.<Message>lambdaQuery()
                .lt(Message::getCreatedAt, before)
                .select(Message::getId));
        if (expired.isEmpty()) {
            return 0;
        }
        List<Long> ids = expired.stream().map(Message::getId).toList();
        messageMapper.deleteByIds(ids);
        return ids.size();
    }
}
