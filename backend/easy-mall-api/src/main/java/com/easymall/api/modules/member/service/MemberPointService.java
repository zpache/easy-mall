package com.easymall.api.modules.member.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.api.entity.AppUser;
import com.easymall.api.entity.MallOrder;
import com.easymall.api.entity.MemberLevel;
import com.easymall.api.entity.PointsLog;
import com.easymall.api.mapper.AppUserMapper;
import com.easymall.api.mapper.MallOrderMapper;
import com.easymall.api.mapper.MemberLevelMapper;
import com.easymall.api.mapper.PointsLogMapper;
import com.easymall.api.modules.setting.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 会员权益：订单支付成功后把消费金额打进会员（积分 / 累计消费 / 订单数 / 等级）。
 *
 * <p>三条硬约束：</p>
 * <ol>
 *   <li>幂等：先按 {@code points_awarded = 0} 条件把订单置为已发放，改成功的那次才累加，
 *       支付重试、补发任务、多实例并发都不会重复计算；</li>
 *   <li>口径统一：累计消费按订单实付金额（payAmount，已含运费、已扣优惠券）累加，
 *       与订单列表展示的「实付」一致；</li>
 *   <li>等级只升不降：按累计消费匹配门槛最高的等级，只有门槛高于当前等级才升级，
 *       不会把运营手工调高的等级降回去。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberPointService {

    /** 会员配置分组（sys_config） */
    private static final String CONFIG_GROUP = "member";
    private static final String KEY_POINTS_ENABLED = "pointsEnabled";
    private static final String KEY_POINTS_RATE = "pointsRate";

    private static final String TYPE_EARN = "earn";

    private final AppUserMapper appUserMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final PointsLogMapper pointsLogMapper;
    private final MallOrderMapper orderMapper;
    private final SysConfigService sysConfigService;

    /**
     * 订单支付成功后发放会员权益。
     *
     * @return true 表示本次真正发放（首次调用），false 表示已发放过或无需发放
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean awardForOrder(MallOrder order) {
        if (order == null || order.getId() == null || order.getUserId() == null) {
            return false;
        }
        // 关积分只影响积分，累计消费与订单数照常累加（否则会员等级会跟着停摆）
        boolean pointsEnabled = sysConfigService.getBoolean(CONFIG_GROUP, KEY_POINTS_ENABLED, true);
        int rate = pointsEnabled
                ? Math.max(sysConfigService.getInt(CONFIG_GROUP, KEY_POINTS_RATE, 1), 0)
                : 0;
        BigDecimal payAmount = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
        int points = rate == 0
                ? 0
                : payAmount.multiply(BigDecimal.valueOf(rate)).setScale(0, RoundingMode.DOWN).intValue();

        // 幂等闸门：只有把 points_awarded 从 0 改成 1 的那一次才继续累加
        int marked = orderMapper.update(null, Wrappers.<MallOrder>lambdaUpdate()
                .eq(MallOrder::getId, order.getId())
                .eq(MallOrder::getPointsAwarded, 0)
                .set(MallOrder::getPointsAwarded, 1)
                .set(MallOrder::getPointsAmount, points));
        if (marked == 0) {
            return false;
        }

        appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
                .eq(AppUser::getId, order.getUserId())
                .setSql("total_spend = total_spend + " + payAmount.toPlainString())
                .setSql("order_count = order_count + 1")
                .setSql("points = points + " + points));

        if (points > 0) {
            recordPointsLog(order, points);
        }
        refreshLevel(order.getUserId());
        return true;
    }

    /**
     * 补发：扫描「已付款但未发放积分」的订单（支付事务后发放失败、历史数据未处理等场景）。
     * 由定时任务调用，返回本次实际发放的订单数。
     */
    @Transactional(rollbackFor = Exception.class)
    public int repairUnawarded() {
        List<MallOrder> orders = orderMapper.selectList(Wrappers.<MallOrder>lambdaQuery()
                .in(MallOrder::getStatus,
                        MallOrder.STATUS_UNDELIVERED, MallOrder.STATUS_UNRECEIVED, MallOrder.STATUS_FINISHED)
                .eq(MallOrder::getPointsAwarded, 0)
                .orderByAsc(MallOrder::getId)
                .last("limit 50"));
        int awarded = 0;
        for (MallOrder order : orders) {
            if (awardForOrder(order)) {
                awarded++;
            }
        }
        return awarded;
    }

    /* ---------------- private ---------------- */

    /** 写积分流水：balance 取累加后的最新积分，便于用户端按时间线核对 */
    private void recordPointsLog(MallOrder order, int points) {
        AppUser user = appUserMapper.selectById(order.getUserId());
        if (user == null) {
            return;
        }
        PointsLog log = new PointsLog();
        log.setUserId(order.getUserId());
        log.setChange(points);
        log.setBalance(user.getPoints() == null ? points : user.getPoints());
        log.setType(TYPE_EARN);
        log.setOrderNo(order.getOrderNo());
        log.setRemark("订单消费获得（实付 " + order.getPayAmount().stripTrailingZeros().toPlainString() + " 元）");
        pointsLogMapper.insert(log);
    }

    /**
     * 按累计消费校正会员等级（只升级）。
     */
    private void refreshLevel(Long userId) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null || user.getTotalSpend() == null) {
            return;
        }
        List<MemberLevel> levels = memberLevelMapper.selectList(Wrappers.<MemberLevel>lambdaQuery()
                .eq(MemberLevel::getStatus, 1)
                .orderByAsc(MemberLevel::getThreshold));
        MemberLevel current = user.getLevelId() == null ? null : memberLevelMapper.selectById(user.getLevelId());

        MemberLevel target = null;
        for (MemberLevel level : levels) {
            BigDecimal threshold = level.getThreshold() == null
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(level.getThreshold());
            if (user.getTotalSpend().compareTo(threshold) >= 0) {
                target = level;
            }
        }
        if (target == null || target.getId().equals(user.getLevelId())) {
            return;
        }
        // 只升不降：目标等级门槛必须高于当前等级门槛
        if (current != null && target.getThreshold() != null && current.getThreshold() != null
                && target.getThreshold() <= current.getThreshold()) {
            return;
        }
        appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
                .eq(AppUser::getId, userId)
                .set(AppUser::getLevelId, target.getId()));
        log.info("会员等级升级: userId={}, {} -> {}, 累计消费={}",
                userId, current == null ? "-" : current.getName(), target.getName(), user.getTotalSpend());
    }
}
