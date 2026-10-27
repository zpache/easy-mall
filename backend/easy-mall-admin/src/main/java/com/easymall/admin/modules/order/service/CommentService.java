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
 * 评价服务：列表、显示 / 隐藏、回复、删除。
 */
@Service
@RequiredArgsConstructor
public class CommentService extends ServiceImpl<OrderCommentMapper, OrderComment> {

    public PageResult<CommentVO> pageQuery(long pageNum, long pageSize, String keyword,
                                           Integer score, Integer isShow) {
        LambdaQueryWrapper<OrderComment> wrapper = Wrappers.<OrderComment>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(OrderComment::getGoodsName, keyword)
                        .or().like(OrderComment::getContent, keyword)
                        .or().like(OrderComment::getOrderNo, keyword)
                        .or().like(OrderComment::getUserNickname, keyword))
                .eq(score != null, OrderComment::getScore, score)
                .eq(isShow != null, OrderComment::getIsShow, isShow)
                .orderByDesc(OrderComment::getCommentedAt)
                .orderByDesc(OrderComment::getId);
        Page<OrderComment> page = page(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page, CommentVO::of);
    }

    /**
     * 显示 / 隐藏评价：隐藏后小程序端不再展示，但数据保留。
     */
    public void updateVisible(Long id, Integer isShow) {
        if (isShow == null || (isShow != 0 && isShow != 1)) {
            throw new BusinessException("显示状态只能为 0（隐藏）或 1（显示）");
        }
        OrderComment comment = getById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
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
