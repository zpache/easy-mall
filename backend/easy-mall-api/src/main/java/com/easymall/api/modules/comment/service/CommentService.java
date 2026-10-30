package com.easymall.api.modules.comment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.api.common.api.PageResult;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.entity.AppUser;
import com.easymall.api.entity.MallOrder;
import com.easymall.api.entity.OrderComment;
import com.easymall.api.entity.OrderItem;
import com.easymall.api.mapper.AppUserMapper;
import com.easymall.api.mapper.MallOrderMapper;
import com.easymall.api.mapper.OrderCommentMapper;
import com.easymall.api.mapper.OrderItemMapper;
import com.easymall.api.modules.comment.dto.CommentCreateForm;
import com.easymall.api.modules.comment.vo.CommentVO;
import com.easymall.api.modules.comment.vo.GoodsCommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评价服务：小程序提交评价、商品详情查评价。
 *
 * <p>规则：只有「已完成」订单能评价，同一订单同一商品只能评价一次；
 * 用户隐藏的（is_show=0）评价仍保留数据但不对外展示。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final int MAX_IMAGES = 3;

    private final OrderCommentMapper commentMapper;
    private final MallOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final AppUserMapper userMapper;

    /**
     * 提交评价。
     *
     * @return 本次新增的评价条数
     */
    @Transactional(rollbackFor = Exception.class)
    public int submit(Long userId, CommentCreateForm form) {
        MallOrder order = orderMapper.selectById(form.orderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != MallOrder.STATUS_FINISHED) {
            throw new BusinessException(ResultCode.COMMENT_ORDER_STATUS_INVALID);
        }

        Map<Long, OrderItem> orderItems = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .eq(OrderItem::getOrderId, order.getId()))
                .stream()
                .collect(Collectors.toMap(OrderItem::getGoodsId, Function.identity(), (a, b) -> a));
        Set<Long> commented = commentMapper.selectList(Wrappers.<OrderComment>lambdaQuery()
                        .eq(OrderComment::getOrderId, order.getId()))
                .stream()
                .map(OrderComment::getGoodsId)
                .collect(Collectors.toSet());

        AppUser user = userMapper.selectById(userId);
        String nickname = user == null ? null : user.getNickname();
        Set<Long> handled = new HashSet<>();
        int created = 0;

        for (CommentCreateForm.Item item : form.items()) {
            OrderItem orderItem = orderItems.get(item.goodsId());
            if (orderItem == null) {
                throw new BusinessException(ResultCode.COMMENT_GOODS_NOT_IN_ORDER);
            }
            // 同一次请求里重复提交同一商品，或此前已评价过
            if (!handled.add(item.goodsId()) || commented.contains(item.goodsId())) {
                throw new BusinessException(ResultCode.COMMENT_ALREADY_DONE, orderItem.getGoodsName() + " 已经评价过了");
            }
            List<String> images = item.images() == null ? List.of() : item.images().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .toList();
            if (images.size() > MAX_IMAGES) {
                throw new BusinessException(ResultCode.COMMENT_IMAGE_LIMIT);
            }

            OrderComment comment = new OrderComment();
            comment.setOrderId(order.getId());
            comment.setOrderNo(order.getOrderNo());
            comment.setGoodsId(orderItem.getGoodsId());
            comment.setGoodsName(orderItem.getGoodsName());
            comment.setGoodsImage(orderItem.getGoodsImage());
            comment.setUserId(userId);
            comment.setUserNickname(nickname);
            comment.setScore(item.score());
            comment.setContent(StringUtils.hasText(item.content()) ? item.content().trim() : null);
            comment.setImages(images.isEmpty() ? null : String.join(",", images));
            comment.setIsShow(1);
            // 新提交一律进待审核：只有管理端审核通过后才对其他用户展示
            comment.setAuditStatus(OrderComment.AUDIT_PENDING);
            comment.setCommentedAt(LocalDateTime.now());
            try {
                commentMapper.insert(comment);
            } catch (DuplicateKeyException e) {
                // 并发重复提交由唯一索引 uk_comment_order_goods 兜底
                throw new BusinessException(ResultCode.COMMENT_ALREADY_DONE, orderItem.getGoodsName() + " 已经评价过了");
            }
            created++;
        }
        return created;
    }

    /**
     * 商品评价列表，附带评分概览。
     *
     * <p>可见性：审核通过（audit_status=1 且 is_show=1）的评价对所有人展示；
     * 登录用户额外能看到自己的待审核 / 已拒绝评价，便于确认提交结果。</p>
     *
     * @param viewerId 当前登录用户，游客为 null
     */
    public GoodsCommentVO goodsComments(Long goodsId, long pageNum, long pageSize, Long viewerId) {
        IPage<OrderComment> page = commentMapper.selectPage(Page.of(pageNum, pageSize),
                Wrappers.<OrderComment>lambdaQuery()
                        .eq(OrderComment::getGoodsId, goodsId)
                        .and(w -> {
                            w.nested(n -> n
                                    .eq(OrderComment::getAuditStatus, OrderComment.AUDIT_PASSED)
                                    .eq(OrderComment::getIsShow, 1));
                            if (viewerId != null) {
                                // 自己的评价：待审核 / 已拒绝未通过时也能看到，通过后又隐藏的仍按隐藏处理
                                w.or(o -> o
                                        .eq(OrderComment::getUserId, viewerId)
                                        .ne(OrderComment::getAuditStatus, OrderComment.AUDIT_PASSED));
                            }
                        })
                        .orderByDesc(OrderComment::getCommentedAt)
                        .orderByDesc(OrderComment::getId));

        Map<String, Object> stat = commentMapper.statByGoods(goodsId);
        long count = 0L;
        BigDecimal avg = BigDecimal.ZERO;
        if (stat != null) {
            Object cnt = stat.get("cnt");
            Object avgScore = stat.get("avgScore");
            count = cnt == null ? 0L : ((Number) cnt).longValue();
            avg = avgScore == null ? BigDecimal.ZERO : new BigDecimal(avgScore.toString());
        }
        return new GoodsCommentVO(
                avg.setScale(1, RoundingMode.HALF_UP),
                count,
                PageResult.of(page, CommentVO::of));
    }

    /**
     * 查询多个订单各自已评价的商品 ID 集合，供订单列表/详情标记「已评价」。
     */
    public Map<Long, Set<Long>> commentedGoodsByOrder(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        return commentMapper.selectList(Wrappers.<OrderComment>lambdaQuery()
                        .select(OrderComment::getOrderId, OrderComment::getGoodsId)
                        .in(OrderComment::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(OrderComment::getOrderId,
                        Collectors.mapping(OrderComment::getGoodsId, Collectors.toSet())));
    }
}
