package com.easymall.api.modules.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.entity.MallOrder;
import com.easymall.api.entity.OrderRefund;
import com.easymall.api.mapper.MallOrderMapper;
import com.easymall.api.mapper.OrderRefundMapper;
import com.easymall.api.modules.order.dto.RefundApplyForm;
import com.easymall.api.modules.order.vo.RefundApplyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    @Transactional(rollbackFor = Exception.class)
    public RefundApplyVO apply(Long userId, RefundApplyForm form) {
        MallOrder order = orderMapper.selectById(form.orderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getPayTime() == null) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单还未支付，无需申请退款，可直接取消订单");
        }
        if (order.getStatus() != null && order.getStatus() == MallOrder.STATUS_CANCELLED) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单已取消，无法申请退款");
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
        return new RefundApplyVO(refund.getId(), refund.getRefundNo(), refund.getAmount(),
                refund.getStatus(), RefundApplyVO.statusText(refund.getStatus()), refund.getAppliedAt());
    }

    private String generateRefundNo() {
        return "RF" + LocalDateTime.now().format(NO_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
