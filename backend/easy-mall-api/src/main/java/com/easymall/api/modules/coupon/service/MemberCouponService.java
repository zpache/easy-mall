package com.easymall.api.modules.coupon.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.entity.Coupon;
import com.easymall.api.entity.CouponUser;
import com.easymall.api.mapper.CouponMapper;
import com.easymall.api.mapper.CouponUserMapper;
import com.easymall.api.modules.coupon.vo.MemberCouponResult;
import com.easymall.api.modules.coupon.vo.MemberCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 我的优惠券（只读）：以领券记录为口径，券模板补齐面额与规则。
 */
@Service
@RequiredArgsConstructor
public class MemberCouponService {

    private static final int STATUS_UNUSED = 0;
    private static final int STATUS_USED = 1;
    private static final int STATUS_EXPIRED = 2;

    private final CouponUserMapper couponUserMapper;
    private final CouponMapper couponMapper;

    /**
     * @param status 可选过滤：0 可用 / 1 已使用 / 2 已过期；null 返回全部
     */
    public MemberCouponResult mine(Integer status) {
        Long userId = UserContext.userId();
        List<CouponUser> records = couponUserMapper.selectList(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .orderByDesc(CouponUser::getId));
        if (records.isEmpty()) {
            return new MemberCouponResult(0, 0, 0, List.of());
        }

        List<Long> couponIds = records.stream().map(CouponUser::getCouponId).distinct().toList();
        Map<Long, Coupon> couponMap = couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                        .in(Coupon::getId, couponIds))
                .stream()
                .collect(Collectors.toMap(Coupon::getId, Function.identity()));

        LocalDateTime now = LocalDateTime.now();
        List<MemberCouponVO> all = records.stream()
                .map(record -> toVO(record, couponMap.get(record.getCouponId()), now))
                // 券模板被删除的记录不再展示
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        int available = (int) all.stream().filter(v -> v.status() == STATUS_UNUSED).count();
        int used = (int) all.stream().filter(v -> v.status() == STATUS_USED).count();
        int expired = (int) all.stream().filter(v -> v.status() == STATUS_EXPIRED).count();

        List<MemberCouponVO> list = status == null
                ? all
                : all.stream().filter(v -> v.status().equals(status)).collect(Collectors.toList());
        list.sort(comparator());

        return new MemberCouponResult(available, used, expired, list);
    }

    /** 可用的按到期时间从近到远，已使用/已过期的按领取记录倒序（最近在前） */
    private Comparator<MemberCouponVO> comparator() {
        return (a, b) -> {
            if (!a.status().equals(b.status())) {
                return Integer.compare(a.status(), b.status());
            }
            if (a.status() == STATUS_UNUSED) {
                LocalDateTime ea = a.expireAt() == null ? LocalDateTime.MAX : a.expireAt();
                LocalDateTime eb = b.expireAt() == null ? LocalDateTime.MAX : b.expireAt();
                return ea.compareTo(eb);
            }
            return Long.compare(b.id(), a.id());
        };
    }

    private MemberCouponVO toVO(CouponUser record, Coupon coupon, LocalDateTime now) {
        if (coupon == null) {
            return null;
        }
        LocalDateTime expireAt = resolveExpireAt(record, coupon);
        int status = normalizeStatus(record.getStatus(), expireAt, now);

        return new MemberCouponVO(
                record.getId(),
                coupon.getId(),
                coupon.getName(),
                coupon.getType(),
                coupon.getAmount(),
                coupon.getDiscount(),
                coupon.getThreshold(),
                status,
                valueText(coupon),
                ruleText(coupon),
                record.getOrderNo(),
                record.getReceivedAt(),
                record.getUsedAt(),
                expireAt);
    }

    /**
     * 到期时间：优先取记录上的 expired_at；
     * 固定日期券取有效期结束当天 23:59:59；领取后 N 天券取领取时间 + N 天。
     */
    private LocalDateTime resolveExpireAt(CouponUser record, Coupon coupon) {
        if (record.getExpiredAt() != null) {
            return record.getExpiredAt();
        }
        if (coupon.getValidType() != null && coupon.getValidType() == 2
                && coupon.getValidDays() != null && record.getReceivedAt() != null) {
            return record.getReceivedAt().plusDays(coupon.getValidDays());
        }
        if (coupon.getValidEnd() != null) {
            return coupon.getValidEnd().atTime(23, 59, 59);
        }
        return null;
    }

    /** 未使用但已过有效期的记录，展示层按“已过期”处理 */
    private int normalizeStatus(Integer rawStatus, LocalDateTime expireAt, LocalDateTime now) {
        int status = rawStatus == null ? STATUS_UNUSED : rawStatus;
        if (status == STATUS_UNUSED && expireAt != null && expireAt.isBefore(now)) {
            return STATUS_EXPIRED;
        }
        return status;
    }

    private String valueText(Coupon coupon) {
        if (coupon.getType() != null && coupon.getType() == 2 && coupon.getDiscount() != null) {
            BigDecimal zhe = coupon.getDiscount().divide(BigDecimal.TEN).stripTrailingZeros();
            return zhe.toPlainString() + "折";
        }
        BigDecimal amount = coupon.getAmount() == null ? BigDecimal.ZERO : coupon.getAmount();
        return "¥" + amount.stripTrailingZeros().toPlainString();
    }

    private String ruleText(Coupon coupon) {
        BigDecimal threshold = coupon.getThreshold();
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
            return "无门槛";
        }
        return "满" + threshold.stripTrailingZeros().toPlainString() + "元可用";
    }
}
