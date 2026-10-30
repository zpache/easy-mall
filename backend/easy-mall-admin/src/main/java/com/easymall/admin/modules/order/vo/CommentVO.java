package com.easymall.admin.modules.order.vo;

import com.easymall.admin.entity.OrderComment;

import java.time.LocalDateTime;

/**
 * 评价列表视图。
 */
public record CommentVO(
        Long id,
        Long orderId,
        String orderNo,
        Long goodsId,
        String goodsName,
        String goodsImage,
        Long userId,
        String userNickname,
        Integer score,
        String content,
        String images,
        String reply,
        LocalDateTime repliedAt,
        Integer isShow,
        Integer auditStatus,
        String auditRemark,
        LocalDateTime auditAt,
        LocalDateTime commentedAt
) {

    public static CommentVO of(OrderComment comment) {
        return new CommentVO(
                comment.getId(),
                comment.getOrderId(),
                comment.getOrderNo(),
                comment.getGoodsId(),
                comment.getGoodsName(),
                comment.getGoodsImage(),
                comment.getUserId(),
                comment.getUserNickname(),
                comment.getScore(),
                comment.getContent(),
                comment.getImages(),
                comment.getReply(),
                comment.getRepliedAt(),
                comment.getIsShow(),
                comment.getAuditStatus(),
                comment.getAuditRemark(),
                comment.getAuditAt(),
                comment.getCommentedAt());
    }
}
