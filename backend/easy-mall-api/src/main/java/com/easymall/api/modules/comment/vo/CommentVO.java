package com.easymall.api.modules.comment.vo;

import com.easymall.api.entity.OrderComment;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 评价展示视图：昵称做脱敏，晒图拆成数组方便前端遍历。
 *
 * @param auditStatus 审核状态：0 待审核，1 已通过，2 已拒绝；非 1 的评价只在本人视角出现
 * @param auditRemark 审核备注（拒绝原因），仅本人视角可见
 */
public record CommentVO(
        Long id,
        Long goodsId,
        String goodsName,
        String goodsImage,
        String nickname,
        Integer score,
        String content,
        List<String> images,
        String reply,
        LocalDateTime repliedAt,
        LocalDateTime commentedAt,
        Integer auditStatus,
        String auditRemark
) {

    public static CommentVO of(OrderComment comment) {
        return new CommentVO(
                comment.getId(),
                comment.getGoodsId(),
                comment.getGoodsName(),
                comment.getGoodsImage(),
                maskNickname(comment.getUserNickname()),
                comment.getScore(),
                comment.getContent(),
                splitImages(comment.getImages()),
                comment.getReply(),
                comment.getRepliedAt(),
                comment.getCommentedAt(),
                comment.getAuditStatus(),
                comment.getAuditRemark());
    }

    public static List<String> splitImages(String images) {
        if (!StringUtils.hasText(images)) {
            return List.of();
        }
        return Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    /**
     * 昵称脱敏：保留首字符 + ***，避免小程序端直接暴露完整微信昵称。
     * 按码点截取，防止 emoji 昵称被截断成乱码。
     */
    public static String maskNickname(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return "匿名用户";
        }
        String trimmed = nickname.trim();
        int end = trimmed.offsetByCodePoints(0, 1);
        return trimmed.substring(0, end) + "***";
    }
}
