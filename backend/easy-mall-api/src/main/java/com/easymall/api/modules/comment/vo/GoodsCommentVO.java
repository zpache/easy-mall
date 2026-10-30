package com.easymall.api.modules.comment.vo;

import com.easymall.api.common.api.PageResult;

import java.math.BigDecimal;

/**
 * 商品评价页数据：评分概览 + 分页评价列表。
 *
 * @param avgScore     平均分（保留 1 位小数），无评价为 0
 * @param commentCount 显示中的评价条数
 */
public record GoodsCommentVO(
        BigDecimal avgScore,
        long commentCount,
        PageResult<CommentVO> page
) {
}
