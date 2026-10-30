package com.easymall.api.modules.member.service;

import com.easymall.api.entity.AppUser;
import com.easymall.api.entity.MemberLevel;
import com.easymall.api.mapper.AppUserMapper;
import com.easymall.api.mapper.MemberLevelMapper;
import com.easymall.api.modules.setting.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 会员等级折扣：把 m_member_level.discount（百分比，98 = 98 折）换算成下单优惠金额。
 *
 * <p>口径：折扣只作用于商品金额（totalAmount），不抵运费；
 * 优惠券门槛与抵扣、运费包邮门槛都按「折后商品金额」计算，
 * 保证结算页展示与下单落库完全一致。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberDiscountService {

    private static final String CONFIG_GROUP = "member";
    private static final String KEY_ENABLED = "levelDiscountEnabled";

    /** 无折扣口径：百分比 100 */
    private static final BigDecimal FULL_RATE = BigDecimal.valueOf(100);

    private final AppUserMapper appUserMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final SysConfigService sysConfigService;

    /**
     * 等级折扣结果。
     *
     * @param levelId        等级 ID，null 表示未识别到等级
     * @param levelName      等级名称（用于订单快照展示）
     * @param discount       折扣百分比（98 表示 98 折）
     * @param discountAmount 商品金额的实际优惠金额
     */
    public record LevelDiscount(Long levelId, String levelName, BigDecimal discount, BigDecimal discountAmount) {

        public static final LevelDiscount NONE =
                new LevelDiscount(null, null, FULL_RATE, BigDecimal.ZERO);

        /** 是否真正产生了优惠 */
        public boolean effective() {
            return discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0;
        }
    }

    /**
     * 计算某用户在本单商品金额下可享受的等级折扣。
     * 关掉开关、无等级、等级停用、折扣不低于 100 时均返回无优惠。
     */
    public LevelDiscount calculate(Long userId, BigDecimal totalAmount) {
        if (userId == null || totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return LevelDiscount.NONE;
        }
        if (!sysConfigService.getBoolean(CONFIG_GROUP, KEY_ENABLED, true)) {
            return LevelDiscount.NONE;
        }
        AppUser user = appUserMapper.selectById(userId);
        if (user == null || user.getLevelId() == null) {
            return LevelDiscount.NONE;
        }
        MemberLevel level = memberLevelMapper.selectById(user.getLevelId());
        if (level == null || !Integer.valueOf(1).equals(level.getStatus())) {
            return LevelDiscount.NONE;
        }
        BigDecimal rate = level.getDiscount() == null ? FULL_RATE : level.getDiscount();
        if (rate.compareTo(FULL_RATE) >= 0 || rate.compareTo(BigDecimal.ZERO) <= 0) {
            // 100 折即无折扣，0 折属于脏数据（会把商品白送），都按无折扣处理
            return new LevelDiscount(level.getId(), level.getName(), FULL_RATE, BigDecimal.ZERO);
        }
        BigDecimal amount = totalAmount
                .multiply(FULL_RATE.subtract(rate))
                .divide(FULL_RATE, 2, RoundingMode.HALF_UP)
                .min(totalAmount);
        return new LevelDiscount(level.getId(), level.getName(), rate, amount);
    }
}
