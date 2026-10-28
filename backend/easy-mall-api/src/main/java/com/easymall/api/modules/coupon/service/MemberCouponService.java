package com.easymall.api.modules.coupon.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.entity.Coupon;
import com.easymall.api.entity.CouponUser;
import com.easymall.api.mapper.CouponMapper;
import com.easymall.api.mapper.CouponUserMapper;
import com.easymall.api.modules.coupon.vo.CouponCardVO;
import com.easymall.api.modules.coupon.vo.MemberCouponResult;
import com.easymall.api.modules.coupon.vo.MemberCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                coupon.getCouponCode(),
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

    /* ========================= 领券中心 ========================= */

    /**
     * 领券中心的券卡片：按装修配置的 code 顺序返回，跳过停用、未开始、已过有效期的券。
     *
     * @param codes 装修组件里选中的券随机码，最多取前 20 个
     */
    public List<CouponCardVO> cards(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        List<String> ordered = codes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(20)
                .toList();
        if (ordered.isEmpty()) {
            return List.of();
        }
        Map<String, Coupon> couponMap = couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                        .in(Coupon::getCouponCode, ordered)
                        .eq(Coupon::getStatus, 1))
                .stream()
                .collect(Collectors.toMap(Coupon::getCouponCode, Function.identity()));

        LocalDate today = LocalDate.now();
        return ordered.stream()
                .map(couponMap::get)
                .filter(Objects::nonNull)
                .filter(coupon -> !isExpired(coupon, today))
                .map(this::toCard)
                .collect(Collectors.toList());
    }

    /**
     * 领取一张券：库存用 SQL 原子占用，避免并发超发。
     * 同一张券的限领校验按“未使用 + 已使用”计数，已过期的记录不占名额。
     */
    @Transactional(rollbackFor = Exception.class)
    public MemberCouponVO receive(String couponCode) {
        Long userId = UserContext.userId();
        if (!StringUtils.hasText(couponCode)) {
            throw new BusinessException(ResultCode.COUPON_NOT_FOUND);
        }
        // 对外只认随机码，自增主键不出网关
        Coupon coupon = couponMapper.selectOne(Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getCouponCode, couponCode.trim())
                .last("limit 1"));
        if (coupon == null) {
            throw new BusinessException(ResultCode.COUPON_NOT_FOUND);
        }
        Long couponId = coupon.getId();
        if (coupon.getStatus() == null || coupon.getStatus() != 1) {
            throw new BusinessException(ResultCode.COUPON_OFFLINE);
        }
        LocalDate today = LocalDate.now();
        if (isNotStarted(coupon, today)) {
            throw new BusinessException(ResultCode.COUPON_EXPIRED, "优惠券还未开始领取");
        }
        if (isExpired(coupon, today)) {
            throw new BusinessException(ResultCode.COUPON_EXPIRED);
        }

        long received = couponUserMapper.selectCount(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getCouponId, couponId)
                .ne(CouponUser::getStatus, STATUS_EXPIRED));
        int perLimit = coupon.getPerLimit() == null || coupon.getPerLimit() <= 0 ? 1 : coupon.getPerLimit();
        if (received >= perLimit) {
            throw new BusinessException(ResultCode.COUPON_LIMIT_REACHED);
        }

        if (couponMapper.increaseSendCount(couponId) == 0) {
            throw new BusinessException(ResultCode.COUPON_SOLD_OUT);
        }

        LocalDateTime now = LocalDateTime.now();
        CouponUser record = new CouponUser();
        record.setCouponId(couponId);
        record.setUserId(userId);
        record.setStatus(STATUS_UNUSED);
        record.setReceivedAt(now);
        record.setExpiredAt(resolveNewExpireAt(coupon, now));
        couponUserMapper.insert(record);

        return toVO(record, coupon, now);
    }

    private CouponCardVO toCard(Coupon coupon) {
        int total = coupon.getTotalCount() == null ? 0 : coupon.getTotalCount();
        int sent = coupon.getSendCount() == null ? 0 : coupon.getSendCount();
        // total_count = 0 视为不限量，remain 返回 -1 由前端隐藏“剩余”角标
        int remain = total <= 0 ? -1 : Math.max(0, total - sent);
        int perLimit = coupon.getPerLimit() == null || coupon.getPerLimit() <= 0 ? 1 : coupon.getPerLimit();
        return new CouponCardVO(
                coupon.getCouponCode(),
                coupon.getName(),
                coupon.getType(),
                valueText(coupon),
                ruleText(coupon),
                validText(coupon),
                total,
                remain,
                perLimit,
                remain == 0);
    }

    private String validText(Coupon coupon) {
        if (coupon.getValidType() != null && coupon.getValidType() == 2) {
            int days = coupon.getValidDays() == null || coupon.getValidDays() <= 0 ? 7 : coupon.getValidDays();
            return "领取后" + days + "天有效";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        if (coupon.getValidStart() != null && coupon.getValidEnd() != null) {
            return coupon.getValidStart().format(formatter) + "-" + coupon.getValidEnd().format(formatter);
        }
        if (coupon.getValidEnd() != null) {
            return "有效期至" + coupon.getValidEnd().format(formatter);
        }
        return "长期有效";
    }

    /** 固定日期券在结束日之后不再可领；领取后 N 天券始终可领 */
    private boolean isExpired(Coupon coupon, LocalDate today) {
        return coupon.getValidType() != null && coupon.getValidType() == 1
                && coupon.getValidEnd() != null && coupon.getValidEnd().isBefore(today);
    }

    private boolean isNotStarted(Coupon coupon, LocalDate today) {
        return coupon.getValidType() != null && coupon.getValidType() == 1
                && coupon.getValidStart() != null && coupon.getValidStart().isAfter(today);
    }

    /** 新领记录的到期时间：固定日期券取结束日 23:59:59，N 天券取领取时间 + N 天 */
    private LocalDateTime resolveNewExpireAt(Coupon coupon, LocalDateTime now) {
        if (coupon.getValidType() != null && coupon.getValidType() == 2) {
            int days = coupon.getValidDays() == null || coupon.getValidDays() <= 0 ? 7 : coupon.getValidDays();
            return now.plusDays(days);
        }
        return coupon.getValidEnd() == null ? null : coupon.getValidEnd().atTime(23, 59, 59);
    }
}
