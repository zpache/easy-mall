package com.easymall.admin.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.context.UserContext;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.AppUser;
import com.easymall.admin.entity.Order;
import com.easymall.admin.entity.OrderRefund;
import com.easymall.admin.mapper.AppUserMapper;
import com.easymall.admin.mapper.OrderMapper;
import com.easymall.admin.mapper.OrderRefundMapper;
import com.easymall.admin.modules.order.dto.RefundAuditForm;
import com.easymall.admin.modules.order.vo.RefundVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 售后退款服务：审核（同意 / 拒绝）与确认打款。
 * <p>
 * 状态流转：0 待审核 → 同意 → 1 待退款 → 确认 → 3 已退款；
 * 0 待审核 → 拒绝 → 2 已拒绝。订单上的 refund_status 同步为 退款中 / 已退款 / 售后被拒。
 */
@Service
@RequiredArgsConstructor
public class RefundService extends ServiceImpl<OrderRefundMapper, OrderRefund> {

    private final OrderMapper orderMapper;
    private final AppUserMapper appUserMapper;

    public PageResult<RefundVO> pageQuery(long pageNum, long pageSize, String keyword, Integer status,
                                          String type) {
        LambdaQueryWrapper<OrderRefund> wrapper = Wrappers.<OrderRefund>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(OrderRefund::getRefundNo, keyword)
                        .or().like(OrderRefund::getOrderNo, keyword)
                        .or().like(OrderRefund::getReason, keyword))
                .eq(status != null, OrderRefund::getStatus, status)
                .eq(StringUtils.hasText(type), OrderRefund::getType, type)
                .orderByDesc(OrderRefund::getAppliedAt)
                .orderByDesc(OrderRefund::getId);
        Page<OrderRefund> page = page(Page.of(pageNum, pageSize), wrapper);
        return new PageResult<>(toVOList(page.getRecords()), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 审核售后单：同意进入待退款，拒绝则结束流程。
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, RefundAuditForm form) {
        if (form.pass() == null) {
            throw new BusinessException("请选择审核结果");
        }
        OrderRefund refund = getById(id);
        if (refund == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (!Objects.equals(refund.getStatus(), OrderRefund.STATUS_PENDING)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "该售后单已处理，请刷新后查看");
        }
        boolean pass = Boolean.TRUE.equals(form.pass());
        refund.setStatus(pass ? OrderRefund.STATUS_APPROVED : OrderRefund.STATUS_REJECTED);
        refund.setAuditRemark(StringUtils.hasText(form.remark())
                ? form.remark()
                : (pass ? "商家已同意退款" : "商家拒绝退款"));
        refund.setHandledAt(LocalDateTime.now());
        refund.setHandledBy(UserContext.userId());
        if (!updateById(refund)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "该售后单已被其他人处理，请刷新后重试");
        }
        updateOrderRefundStatus(refund.getOrderId(),
                pass ? Order.REFUND_PROCESSING : Order.REFUND_REJECTED);
    }

    /**
     * 确认退款：财务打款成功后调用，退款单与订单同时置为已退款。
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        OrderRefund refund = getById(id);
        if (refund == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (!Objects.equals(refund.getStatus(), OrderRefund.STATUS_APPROVED)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "只有已同意待退款的售后单才能确认打款");
        }
        refund.setStatus(OrderRefund.STATUS_REFUNDED);
        refund.setHandledAt(LocalDateTime.now());
        refund.setHandledBy(UserContext.userId());
        if (!updateById(refund)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "该售后单已被其他人处理，请刷新后重试");
        }
        updateOrderRefundStatus(refund.getOrderId(), Order.REFUND_DONE);
    }

    private void updateOrderRefundStatus(Long orderId, int refundStatus) {
        if (orderId == null) {
            return;
        }
        orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, orderId)
                .set(Order::getRefundStatus, refundStatus)
                .set(Order::getUpdatedAt, LocalDateTime.now()));
    }

    private List<RefundVO> toVOList(List<OrderRefund> refunds) {
        if (refunds == null || refunds.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = refunds.stream().map(OrderRefund::getOrderId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Order> orderMap = orderIds.isEmpty()
                ? Map.of()
                : orderMapper.selectList(Wrappers.<Order>lambdaQuery().in(Order::getId, orderIds))
                .stream().collect(Collectors.toMap(Order::getId, order -> order, (a, b) -> a));
        Set<Long> userIds = refunds.stream().map(OrderRefund::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AppUser> userMap = userIds.isEmpty()
                ? Map.of()
                : appUserMapper.selectList(Wrappers.<AppUser>lambdaQuery().in(AppUser::getId, userIds))
                .stream().collect(Collectors.toMap(AppUser::getId, user -> user, (a, b) -> a));
        return refunds.stream().map(refund -> {
            AppUser user = refund.getUserId() == null ? null : userMap.get(refund.getUserId());
            return RefundVO.of(refund, orderMap.get(refund.getOrderId()),
                    user == null ? null : user.getNickname());
        }).toList();
    }
}
