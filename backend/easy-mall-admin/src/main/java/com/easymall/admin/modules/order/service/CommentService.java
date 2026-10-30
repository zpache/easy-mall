package com.easymall.admin.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.OrderComment;
import com.easymall.admin.mapper.OrderCommentMapper;
import com.easymall.admin.modules.order.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 评价服务：列表、审核、显示 / 隐藏、回复、删除。
 */
@Service
@RequiredArgsConstructor
public class CommentService extends ServiceImpl<OrderCommentMapper, OrderComment> {

    public PageResult<CommentVO> pageQuery(long pageNum, long pageSize, String keyword,
                                           Integer score, Integer isShow, Integer auditStatus) {
        LambdaQueryWrapper<OrderComment> wrapper = Wrappers.<OrderComment>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(OrderComment::getGoodsName, keyword)
                        .or().like(OrderComment::getContent, keyword)
                        .or().like(OrderComment::getOrderNo, keyword)
                        .or().like(OrderComment::getUserNickname, keyword))
                .eq(score != null, OrderComment::getScore, score)
                .eq(isShow != null, OrderComment::getIsShow, isShow)
                .eq(auditStatus != null, OrderComment::getAuditStatus, auditStatus)
                .orderByDesc(OrderComment::getCommentedAt)
                .orderByDesc(OrderComment::getId);
        Page<OrderComment> page = page(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page, CommentVO::of);
    }

    /**
     * 待审核数量：列表页提示用。
     */
    public long pendingCount() {
        return count(Wrappers.<OrderComment>lambdaQuery()
                .eq(OrderComment::getAuditStatus, OrderComment.AUDIT_PENDING));
    }

    /**
     * 审核评价：通过后小程序端其他用户可见，拒绝则只有评价本人能看到（拒绝同时下架）。
     * 仅「待审核」的记录可审核，条件更新保证并发下只有一人成功，已审核的不能再改判。
     */
    public void audit(Long id, Integer status, String remark) {
        if (status == null
                || (status != OrderComment.AUDIT_PASSED && status != OrderComment.AUDIT_REJECTED)) {
            throw new BusinessException("审核结果只能为 1（通过）或 2（拒绝）");
        }
        int affected = baseMapper.update(null, Wrappers.<OrderComment>lambdaUpdate()
                .eq(OrderComment::getId, id)
                .eq(OrderComment::getAuditStatus, OrderComment.AUDIT_PENDING)
                .set(OrderComment::getAuditStatus, status)
                .set(OrderComment::getAuditRemark, StringUtils.hasText(remark) ? remark.trim() : null)
                .set(OrderComment::getAuditAt, LocalDateTime.now())
                .set(status == OrderComment.AUDIT_REJECTED, OrderComment::getIsShow, 0));
        if (affected == 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "该评价不存在或已被审核，请刷新后查看");
        }
    }

    /**
     * 显示 / 隐藏评价：隐藏后小程序端不再展示，但数据保留。
     * 只有审核通过的评价才有展示资格，未审核的先审核。
     */
    public void updateVisible(Long id, Integer isShow) {
        if (isShow == null || (isShow != 0 && isShow != 1)) {
            throw new BusinessException("显示状态只能为 0（隐藏）或 1（显示）");
        }
        OrderComment comment = getById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (comment.getAuditStatus() == null || comment.getAuditStatus() != OrderComment.AUDIT_PASSED) {
            throw new BusinessException("评价尚未审核通过，请先审核再设置显示状态");
        }
        comment.setIsShow(isShow);
        if (!updateById(comment)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /**
     * 商家回复：可多次覆盖回复内容。
     */
    public void reply(Long id, String reply) {
        OrderComment comment = getById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        comment.setReply(reply);
        comment.setRepliedAt(LocalDateTime.now());
        if (!updateById(comment)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    public void delete(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
    }
}
