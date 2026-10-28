package com.easymall.admin.modules.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.AppUser;
import com.easymall.admin.entity.Coupon;
import com.easymall.admin.entity.CouponUser;
import com.easymall.admin.mapper.AppUserMapper;
import com.easymall.admin.mapper.CouponMapper;
import com.easymall.admin.mapper.CouponUserMapper;
import com.easymall.admin.modules.marketing.dto.CouponForm;
import com.easymall.admin.modules.marketing.support.CouponCodeGenerator;
import com.easymall.admin.modules.marketing.vo.CouponUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 优惠券：券模板维护与领券记录查询。
 */
@Service
@RequiredArgsConstructor
public class CouponService extends ServiceImpl<CouponMapper, Coupon> {

    private final CouponUserMapper couponUserMapper;
    private final AppUserMapper appUserMapper;

    public PageResult<Coupon> pageQuery(long pageNum, long pageSize, String keyword, Integer type, Integer status) {
        LambdaQueryWrapper<Coupon> wrapper = Wrappers.<Coupon>lambdaQuery()
                .like(StringUtils.hasText(keyword), Coupon::getName, keyword)
                .eq(type != null, Coupon::getType, type)
                .eq(status != null, Coupon::getStatus, status)
                .orderByDesc(Coupon::getId);
        Page<Coupon> page = page(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    public Coupon create(CouponForm form) {
        validateForm(form);
        Coupon coupon = new Coupon();
        applyForm(coupon, form);
        coupon.setCouponCode(generateUniqueCode());
        coupon.setSendCount(0);
        coupon.setUsedCount(0);
        save(coupon);
        return coupon;
    }

    /**
     * 生成对外随机码并查重；极端碰撞时重试，库表唯一索引 uk_coupon_code 兜底。
     */
    private String generateUniqueCode() {
        for (int i = 0; i < 5; i++) {
            String code = CouponCodeGenerator.next();
            boolean exists = count(Wrappers.<Coupon>lambdaQuery().eq(Coupon::getCouponCode, code)) > 0;
            if (!exists) {
                return code;
            }
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "券码生成失败，请重试");
    }

    public Coupon update(Long id, CouponForm form) {
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        validateForm(form);
        // 已有领取记录的券不允许改类型与面额结构，避免存量券口径不一致
        if (!coupon.getType().equals(form.type()) && coupon.getSendCount() > 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "该券已有领取记录，不能修改券类型");
        }
        applyForm(coupon, form);
        if (form.version() != null) {
            coupon.setVersion(form.version());
        }
        if (!updateById(coupon)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return coupon;
    }

    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值只能为 0（停用）或 1（启用）");
        }
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        coupon.setStatus(status);
        if (!updateById(coupon)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    public void delete(Long id) {
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        long received = couponUserMapper.selectCount(
                Wrappers.<CouponUser>lambdaQuery().eq(CouponUser::getCouponId, id));
        if (received > 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT,
                    "该券已被领取 " + received + " 次，请先停用而不建议删除");
        }
        removeById(id);
    }

    /** 领券记录分页：可按券、会员、核销状态过滤。 */
    public PageResult<CouponUserVO> pageRecords(long pageNum, long pageSize,
                                                Long couponId, Long userId, Integer status) {
        LambdaQueryWrapper<CouponUser> wrapper = Wrappers.<CouponUser>lambdaQuery()
                .eq(couponId != null, CouponUser::getCouponId, couponId)
                .eq(userId != null, CouponUser::getUserId, userId)
                .eq(status != null, CouponUser::getStatus, status)
                .orderByDesc(CouponUser::getId);
        Page<CouponUser> page = couponUserMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        List<CouponUser> records = page.getRecords();
        Map<Long, String> couponNames = couponNamesOf(
                records.stream().map(CouponUser::getCouponId).collect(Collectors.toSet()));
        Map<Long, String> nicknames = nicknamesOf(
                records.stream().map(CouponUser::getUserId).collect(Collectors.toSet()));
        List<CouponUserVO> rows = records.stream()
                .map(record -> new CouponUserVO(
                        record.getId(), record.getCouponId(), couponNames.get(record.getCouponId()),
                        record.getUserId(), nicknames.get(record.getUserId()), record.getStatus(),
                        record.getOrderNo(), record.getReceivedAt(), record.getUsedAt()))
                .toList();
        return new PageResult<>(rows, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /** 类型口径校验：满减/无门槛必须面额，折扣必须折扣率，固定日期必须起止。 */
    private void validateForm(CouponForm form) {
        if (form.type() == 2) {
            if (form.discount() == null) {
                throw new BusinessException("折扣券必须填写折扣");
            }
            if (form.threshold() == null || form.threshold().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("折扣券必须设置使用门槛");
            }
        } else {
            if (form.amount() == null || form.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("满减券 / 无门槛券必须填写面额");
            }
            if (form.threshold() == null || form.threshold().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("使用门槛不能为负数");
            }
        }
        if (form.validType() == 1) {
            if (form.validStart() == null || form.validEnd() == null
                    || !form.validEnd().isAfter(form.validStart())) {
                throw new BusinessException("固定日期有效期必须填写开始与结束日期，且结束晚于开始");
            }
        } else if (form.validDays() == null) {
            throw new BusinessException("请填写领取后有效天数");
        }
    }

    private void applyForm(Coupon coupon, CouponForm form) {
        coupon.setName(form.name());
        coupon.setType(form.type());
        coupon.setAmount(form.amount());
        coupon.setDiscount(form.discount());
        coupon.setThreshold(form.threshold() == null ? BigDecimal.ZERO : form.threshold());
        coupon.setTotalCount(form.totalCount());
        coupon.setPerLimit(form.perLimit() == null ? 1 : form.perLimit());
        coupon.setValidType(form.validType());
        coupon.setValidStart(form.validStart());
        coupon.setValidEnd(form.validEnd());
        coupon.setValidDays(form.validDays());
        coupon.setStatus(form.status() == null ? 1 : form.status());
        coupon.setRemark(form.remark());
    }

    private Map<Long, String> couponNamesOf(Collection<Long> couponIds) {
        if (couponIds.isEmpty()) {
            return Map.of();
        }
        return listByIds(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, Coupon::getName, (a, b) -> a));
    }

    private Map<Long, String> nicknamesOf(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return appUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(AppUser::getId,
                        user -> user.getNickname() == null ? "会员" + user.getId() : user.getNickname(),
                        (a, b) -> a));
    }
}
