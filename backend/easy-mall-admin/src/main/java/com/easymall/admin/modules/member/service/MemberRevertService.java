package com.easymall.admin.modules.member.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.admin.entity.AppUser;
import com.easymall.admin.entity.MemberLevel;
import com.easymall.admin.entity.Order;
import com.easymall.admin.entity.OrderRefund;
import com.easymall.admin.entity.PointsLog;
import com.easymall.admin.mapper.AppUserMapper;
import com.easymall.admin.mapper.MemberLevelMapper;
import com.easymall.admin.mapper.OrderMapper;
import com.easymall.admin.mapper.PointsLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 退款回滚会员权益：把订单发过的积分、累计消费、订单数按退款金额退回去。
 *
 * <p>口径：</p>
 * <ol>
 *   <li>只处理已发放过会员权益的订单（points_awarded = 1），未付款订单没有可回滚的；</li>
 *   <li>按退款金额占实付金额的比例回滚积分（向下取整），整单退完时一次回滚干净；</li>
 *   <li>累计消费按本次退款金额直接扣减，累计订单数只在「整单退完」时减 1；</li>
 *   <li>幂等：以订单上的 refunded_amount 做条件更新，同一笔退款重复确认、多张退款单并发都不会重复扣减；</li>
 *   <li>等级按回滚后的累计消费重算（退款可以降级，这是与「消费升级只升不降」的差异点）。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberRevertService {

    private static final String TYPE_REFUND = "refund";

    private final OrderMapper orderMapper;
    private final AppUserMapper appUserMapper;
    private final PointsLogMapper pointsLogMapper;
    private final MemberLevelMapper memberLevelMapper;

    /**
     * 退款确认打款后调用：回滚该笔退款对应的会员权益。
     *
     * @param refund 已置为「已退款」的售后单
     */
    @Transactional(rollbackFor = Exception.class)
    public void revertForRefund(OrderRefund refund) {
        if (refund == null || refund.getOrderId() == null || refund.getAmount() == null) {
            return;
        }
        Order order = orderMapper.selectById(refund.getOrderId());
        if (order == null) {
            return;
        }
        BigDecimal payAmount = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal refundedBefore = order.getRefundedAmount() == null
                ? BigDecimal.ZERO : order.getRefundedAmount();
        BigDecimal remain = payAmount.subtract(refundedBefore).max(BigDecimal.ZERO);
        BigDecimal thisRefund = refund.getAmount().min(remain);
        if (thisRefund.compareTo(BigDecimal.ZERO) <= 0) {
            // 订单已全额退过，重复确认不再重复扣减
            return;
        }

        boolean awarded = order.getPointsAwarded() != null && order.getPointsAwarded() == 1;
        int awardedPoints = order.getPointsAmount() == null ? 0 : order.getPointsAmount();
        int revertedBefore = order.getPointsReverted() == null ? 0 : order.getPointsReverted();
        BigDecimal refundedAfter = refundedBefore.add(thisRefund);
        boolean fullyRefunded = refundedAfter.compareTo(payAmount) >= 0;

        // 应回滚积分总量：整单退完一次清空，部分退款按累计退款比例向下取整
        int shouldRevert = fullyRefunded
                ? awardedPoints
                : BigDecimal.valueOf(awardedPoints)
                .multiply(refundedAfter)
                .divide(payAmount, 0, RoundingMode.DOWN)
                .intValue();
        int revertPoints = awarded ? Math.max(shouldRevert - revertedBefore, 0) : 0;

        // 幂等闸门：以 refunded_amount 原值做条件更新，抢到的那一次才真正扣减
        var orderUpdate = Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, order.getId())
                .eq(Order::getRefundedAmount, refundedBefore)
                .set(Order::getRefundedAmount, refundedAfter);
        if (awarded) {
            orderUpdate.set(Order::getPointsReverted, revertedBefore + revertPoints);
        } else {
            // 积分还没发就退款：顺手把订单标成「已处理、发 0 分」，避免 api 补发任务事后又把积分补上
            orderUpdate.eq(Order::getPointsAwarded, 0)
                    .set(Order::getPointsAwarded, 1)
                    .set(Order::getPointsAmount, 0);
        }
        if (orderMapper.update(null, orderUpdate) == 0) {
            log.warn("退款回滚会员权益跳过（订单金额已被其他退款单更新）: orderNo={}, refundNo={}",
                    order.getOrderNo(), refund.getRefundNo());
            return;
        }
        if (!awarded) {
            log.info("退款发生在积分发放之前，已拦截事后补发: orderNo={}, refundNo={}",
                    order.getOrderNo(), refund.getRefundNo());
            return;
        }

        var memberUpdate = Wrappers.<AppUser>lambdaUpdate()
                .eq(AppUser::getId, order.getUserId())
                .setSql("points = GREATEST(points - " + revertPoints + ", 0)")
                .setSql("total_spend = GREATEST(total_spend - " + thisRefund.toPlainString() + ", 0)");
        if (fullyRefunded) {
            // 累计订单数只在整单退完时减 1，部分退款不改变订单笔数
            memberUpdate.setSql("order_count = GREATEST(order_count - 1, 0)");
        }
        int updated = appUserMapper.update(null, memberUpdate);
        if (updated == 0) {
            // 会员不存在（历史脏数据）不应阻断退款：钱已经打出去了，这里只记日志便于人工核对
            log.warn("退款权益回滚未命中会员: orderNo={}, userId={}", order.getOrderNo(), order.getUserId());
            return;
        }

        if (revertPoints > 0) {
            recordRefundLog(order, refund, revertPoints);
        }
        refreshLevel(order.getUserId());

        log.info("退款回滚会员权益: orderNo={}, refundNo={}, 退款={}, 回滚积分={}, 整单退完={}",
                order.getOrderNo(), refund.getRefundNo(), thisRefund, revertPoints, fullyRefunded);
    }

    /** 退款扣减积分流水：change 为负值，备注里带退款单号便于与财务对账 */
    private void recordRefundLog(Order order, OrderRefund refund, int revertPoints) {
        AppUser user = appUserMapper.selectById(order.getUserId());
        if (user == null) {
            return;
        }
        PointsLog pointsLog = new PointsLog();
        pointsLog.setUserId(order.getUserId());
        pointsLog.setChange(-revertPoints);
        pointsLog.setBalance(user.getPoints() == null ? 0 : user.getPoints());
        pointsLog.setType(TYPE_REFUND);
        pointsLog.setOrderNo(order.getOrderNo());
        pointsLog.setRemark("退款扣回（退款单 " + refund.getRefundNo()
                + "，退款 " + refund.getAmount().stripTrailingZeros().toPlainString() + " 元）");
        pointsLogMapper.insert(pointsLog);
    }

    /**
     * 按当前累计消费重算会员等级。退款场景允许降级，取门槛不高于累计消费的最高启用等级。
     */
    private void refreshLevel(Long userId) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null || user.getTotalSpend() == null) {
            return;
        }
        List<MemberLevel> levels = memberLevelMapper.selectList(Wrappers.<MemberLevel>lambdaQuery()
                .eq(MemberLevel::getStatus, 1)
                .orderByAsc(MemberLevel::getThreshold));
        MemberLevel target = null;
        for (MemberLevel level : levels) {
            BigDecimal threshold = level.getThreshold() == null
                    ? BigDecimal.ZERO : BigDecimal.valueOf(level.getThreshold());
            if (user.getTotalSpend().compareTo(threshold) >= 0) {
                target = level;
            }
        }
        if (target == null || target.getId().equals(user.getLevelId())) {
            return;
        }
        appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
                .eq(AppUser::getId, userId)
                .set(AppUser::getLevelId, target.getId()));
        log.info("退款后会员等级调整: userId={}, {} -> {}, 累计消费={}",
                userId, user.getLevelId(), target.getId(), user.getTotalSpend());
    }
}
