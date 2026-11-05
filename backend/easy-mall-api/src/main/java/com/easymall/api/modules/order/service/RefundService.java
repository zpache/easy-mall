package com.easymall.api.modules.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.api.common.api.PageResult;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.entity.MallOrder;
import com.easymall.api.entity.Message;
import com.easymall.api.entity.OrderRefund;
import com.easymall.api.mapper.MallOrderMapper;
import com.easymall.api.mapper.OrderRefundMapper;
import com.easymall.api.modules.message.service.MessageWriter;
import com.easymall.api.modules.order.dto.RefundApplyForm;
import com.easymall.api.modules.order.vo.RefundApplyVO;
import com.easymall.api.modules.order.vo.RefundRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 售后申请：小程序端发起，落到 o_order_refund 待审核，后台审核 → 原路退款。
 *
 * <p>金额不由前端传入，服务端按「实付 − 已退」计算，避免超额申请；
 * 同一订单同时只允许一笔处理中的售后。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 订单上的售后状态：1 退款中（与后台 Order.REFUND_PROCESSING 一致） */
    private static final int REFUND_PROCESSING = 1;

    private final OrderRefundMapper refundMapper;
    private final MallOrderMapper orderMapper;
    private final MessageWriter messageWriter;

    @Transactional(rollbackFor = Exception.class)
    public RefundApplyVO apply(Long userId, RefundApplyForm form) {
        MallOrder order = orderMapper.selectById(form.orderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getPayTime() == null) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单还未支付，无需申请退款，可直接取消订单");
        }
        // 只有「已支付、待发货」可申请退款：货已在途/已签收的售后走线下协商或客服渠道，
        // 与小程序端入口展示口径一致，避免已发货订单直接原路退回造成钱货两失
        if (order.getStatus() == null || order.getStatus() != MallOrder.STATUS_UNDELIVERED) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "只有已支付、待发货的订单可以申请退款");
        }

        List<OrderRefund> running = refundMapper.selectList(Wrappers.<OrderRefund>lambdaQuery()
                .eq(OrderRefund::getOrderId, order.getId())
                .in(OrderRefund::getStatus, OrderRefund.STATUS_PENDING, OrderRefund.STATUS_APPROVED));
        if (!running.isEmpty()) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "该订单已有处理中的退款申请，请等待商家处理");
        }

        BigDecimal payAmount = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
        BigDecimal refunded = order.getRefundedAmount() == null ? BigDecimal.ZERO : order.getRefundedAmount();
        BigDecimal remain = payAmount.subtract(refunded);
        if (remain.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "该订单已全额退款");
        }

        OrderRefund refund = new OrderRefund();
        refund.setRefundNo(generateRefundNo());
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        refund.setUserId(userId);
        refund.setAmount(remain);
        refund.setType(StringUtils.hasText(form.type()) ? form.type() : "refund");
        refund.setReason(StringUtils.hasText(form.reason()) ? form.reason().trim() : "用户申请退款");
        refund.setStatus(OrderRefund.STATUS_PENDING);
        refund.setAppliedAt(LocalDateTime.now());
        refundMapper.insert(refund);

        // 订单标记为退款中，后台列表一眼能看到
        orderMapper.update(null, Wrappers.<MallOrder>lambdaUpdate()
                .eq(MallOrder::getId, order.getId())
                .set(MallOrder::getRefundStatus, REFUND_PROCESSING));

        log.info("用户申请退款: orderNo={}, refundNo={}, 金额={}", order.getOrderNo(), refund.getRefundNo(), remain);
        // 通知后台审核：消息写失败不影响售后单本身
        try {
            messageWriter.publish(
                    Message.TYPE_REFUND_APPLY,
                    Message.LEVEL_WARN,
                    "新的退款申请",
                    "订单 " + order.getOrderNo() + " 申请退款 ¥" + remain + "，原因：" + refund.getReason(),
                    "refund",
                    refund.getId(),
                    "/order/refund",
                    Message.TYPE_REFUND_APPLY + ":" + refund.getId());
        } catch (Exception e) {
            log.warn("写入退款申请消息失败: refundNo={}", refund.getRefundNo(), e);
        }
        return new RefundApplyVO(refund.getId(), refund.getRefundNo(), refund.getAmount(),
                refund.getStatus(), RefundApplyVO.statusText(refund.getStatus()), refund.getAppliedAt());
    }

    private String generateRefundNo() {
        return "RF" + LocalDateTime.now().format(NO_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /**
     * 我的售后分页：status 为空查全部，其余按售后单状态过滤。
     */
    public PageResult<RefundRecordVO> pageMy(Long userId, long pageNum, long pageSize, Integer status) {
        Page<OrderRefund> page = refundMapper.selectPage(Page.of(pageNum, pageSize),
                Wrappers.<OrderRefund>lambdaQuery()
                        .eq(OrderRefund::getUserId, userId)
                        .eq(status != null, OrderRefund::getStatus, status)
                        .orderByDesc(OrderRefund::getAppliedAt)
                        .orderByDesc(OrderRefund::getId));
        Map<Long, MallOrder> orderMap = loadOrders(page.getRecords());
        return PageResult.of(page, refund -> RefundRecordVO.of(refund, orderMap.get(refund.getOrderId())));
    }

    /**
     * 售后详情（含进度）：只能看自己的售后单。
     */
    public RefundRecordVO detail(Long userId, Long id) {
        OrderRefund refund = refundMapper.selectById(id);
        if (refund == null || !refund.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "售后单不存在");
        }
        return RefundRecordVO.of(refund, orderMapper.selectById(refund.getOrderId()));
    }

    /**
     * 撤销售后申请：仅待审核可撤销，撤销后订单售后状态还原为「无售后」，可重新申请。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long id) {
        OrderRefund refund = refundMapper.selectById(id);
        if (refund == null || !refund.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "售后单不存在");
        }
        if (!Objects.equals(refund.getStatus(), OrderRefund.STATUS_PENDING)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "只有待商家审核的售后单可以撤销");
        }
        refund.setStatus(OrderRefund.STATUS_CANCELLED);
        refund.setHandledAt(LocalDateTime.now());
        if (refundMapper.updateById(refund) <= 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "撤销失败，请刷新后重试");
        }
        // 订单售后标记还原为无售后：用户随后可重新发起申请
        orderMapper.update(null, Wrappers.<MallOrder>lambdaUpdate()
                .eq(MallOrder::getId, refund.getOrderId())
                .set(MallOrder::getRefundStatus, 0));
        log.info("用户撤销售后申请: orderNo={}, refundNo={}", refund.getOrderNo(), refund.getRefundNo());
    }

    private Map<Long, MallOrder> loadOrders(List<OrderRefund> refunds) {
        List<Long> orderIds = refunds.stream().map(OrderRefund::getOrderId)
                .filter(Objects::nonNull).distinct().toList();
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return orderMapper.selectList(Wrappers.<MallOrder>lambdaQuery().in(MallOrder::getId, orderIds))
                .stream().collect(Collectors.toMap(MallOrder::getId, Function.identity(), (a, b) -> a));
    }
}
