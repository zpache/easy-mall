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
import com.easymall.admin.modules.member.service.MemberRevertService;
import com.easymall.admin.modules.order.dto.RefundAuditForm;
import com.easymall.admin.modules.order.vo.RefundVO;
import com.easymall.admin.modules.pay.service.WxRefundService;
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
    private final MemberRevertService memberRevertService;
    private final WxRefundService wxRefundService;
    private final OrderService orderService;

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
     * 确认退款：调用微信支付原路退款，成功后退款单与订单同时置为已退款。
     *
     * <p>退款调用在事务最前面：微信退款失败直接抛异常，状态与会员权益都不会变动，
     * 运营处理失败原因后可以重试，不会出现「状态已退款、钱没到账」。</p>
     *
     * <p>同时回滚该订单已发放的会员权益（积分 / 累计消费 / 订单数 / 等级），与打款同一事务。</p>
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
        Order order = orderMapper.selectById(refund.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "订单不存在，无法退款");
        }

        // 真实退款（未启用微信支付时按模拟退款处理，返回 null）
        String wxRefundNo = wxRefundService.refund(order, refund);

        refund.setWxRefundNo(wxRefundNo);
        refund.setStatus(OrderRefund.STATUS_REFUNDED);
        refund.setHandledAt(LocalDateTime.now());
        refund.setHandledBy(UserContext.userId());
        if (!updateById(refund)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "该售后单已被其他人处理，请刷新后重试");
        }
        updateOrderRefundStatus(refund.getOrderId(), Order.REFUND_DONE);
        // 权益回滚与打款状态同一事务：回滚失败时整个确认操作回滚，运营重试即可，不会出现「钱退了积分没扣」
        memberRevertService.revertForRefund(refund);
        // 全额退款且尚未发货：自动关闭订单并回滚库存与优惠券，避免运营按「待发货」筛选时误发货
        orderService.closeRefundedOrder(orderMapper.selectById(refund.getOrderId()));
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
