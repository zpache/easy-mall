package com.easymall.admin.modules.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.Promotion;
import com.easymall.admin.mapper.PromotionMapper;
import com.easymall.admin.modules.marketing.dto.PromotionForm;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 拼团秒杀活动。
 */
@Service
public class PromotionService extends ServiceImpl<PromotionMapper, Promotion> {

    public PageResult<Promotion> pageQuery(long pageNum, long pageSize, String keyword, Integer type, Integer status) {
        LambdaQueryWrapper<Promotion> wrapper = Wrappers.<Promotion>lambdaQuery()
                .like(StringUtils.hasText(keyword), Promotion::getName, keyword)
                .eq(type != null, Promotion::getType, type)
                .eq(status != null, Promotion::getStatus, status)
                .orderByDesc(Promotion::getStartTime)
                .orderByDesc(Promotion::getId);
        Page<Promotion> page = page(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    public Promotion create(PromotionForm form) {
        validateForm(form);
        Promotion promotion = new Promotion();
        applyForm(promotion, form);
        promotion.setSoldCount(0);
        save(promotion);
        return promotion;
    }

    public Promotion update(Long id, PromotionForm form) {
        Promotion promotion = getById(id);
        if (promotion == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        validateForm(form);
        if (form.stock() < promotion.getSoldCount()) {
            throw new BusinessException("活动库存不能小于已售数量 " + promotion.getSoldCount());
        }
        applyForm(promotion, form);
        if (form.version() != null) {
            promotion.setVersion(form.version());
        }
        if (!updateById(promotion)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return promotion;
    }

    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值只能为 0（停用）或 1（启用）");
        }
        Promotion promotion = getById(id);
        if (promotion == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        promotion.setStatus(status);
        if (!updateById(promotion)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    public void delete(Long id) {
        Promotion promotion = getById(id);
        if (promotion == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (promotion.getSoldCount() > 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT,
                    "该活动已售 " + promotion.getSoldCount() + " 件，请先停用而不建议删除");
        }
        removeById(id);
    }

    private void validateForm(PromotionForm form) {
        if (form.promoPrice().compareTo(form.originalPrice()) >= 0) {
            throw new BusinessException("活动价必须低于原价");
        }
        if (form.type() == 2) {
            if (form.groupSize() == null || form.groupSize() < 2) {
                throw new BusinessException("拼团活动必须设置成团人数（至少 2 人）");
            }
            if (form.groupHours() == null || form.groupHours() < 1) {
                throw new BusinessException("拼团活动必须设置成团有效时长");
            }
        }
        if (!form.endTime().isAfter(form.startTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
    }

    private void applyForm(Promotion promotion, PromotionForm form) {
        promotion.setName(form.name());
        promotion.setType(form.type());
        promotion.setGoodsId(form.goodsId());
        promotion.setGoodsName(form.goodsName());
        promotion.setOriginalPrice(form.originalPrice());
        promotion.setPromoPrice(form.promoPrice());
        promotion.setGroupSize(form.type() == 2 ? form.groupSize() : null);
        promotion.setGroupHours(form.type() == 2 ? form.groupHours() : null);
        promotion.setStock(form.stock());
        promotion.setStartTime(form.startTime());
        promotion.setEndTime(form.endTime());
        promotion.setStatus(form.status() == null ? 1 : form.status());
    }
}
