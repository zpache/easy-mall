package com.easymall.admin.modules.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.Reduction;
import com.easymall.admin.mapper.ReductionMapper;
import com.easymall.admin.modules.marketing.dto.ReductionForm;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 满减活动。
 */
@Service
public class ReductionService extends ServiceImpl<ReductionMapper, Reduction> {

    public PageResult<Reduction> pageQuery(long pageNum, long pageSize, String keyword, Integer mode, Integer status) {
        LambdaQueryWrapper<Reduction> wrapper = Wrappers.<Reduction>lambdaQuery()
                .like(StringUtils.hasText(keyword), Reduction::getName, keyword)
                .eq(mode != null, Reduction::getMode, mode)
                .eq(status != null, Reduction::getStatus, status)
                .orderByDesc(Reduction::getStartTime)
                .orderByDesc(Reduction::getId);
        Page<Reduction> page = page(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    public Reduction create(ReductionForm form) {
        validateForm(form);
        Reduction reduction = new Reduction();
        applyForm(reduction, form);
        save(reduction);
        return reduction;
    }

    public Reduction update(Long id, ReductionForm form) {
        Reduction reduction = getById(id);
        if (reduction == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        validateForm(form);
        applyForm(reduction, form);
        if (form.version() != null) {
            reduction.setVersion(form.version());
        }
        if (!updateById(reduction)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return reduction;
    }

    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值只能为 0（停用）或 1（启用）");
        }
        Reduction reduction = getById(id);
        if (reduction == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        reduction.setStatus(status);
        if (!updateById(reduction)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    public void delete(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        removeById(id);
    }

    private void validateForm(ReductionForm form) {
        if (form.benefit().compareTo(form.threshold()) >= 0) {
            throw new BusinessException("满减金额必须小于满减门槛");
        }
        if (!form.endTime().isAfter(form.startTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
    }

    private void applyForm(Reduction reduction, ReductionForm form) {
        reduction.setName(form.name());
        reduction.setMode(form.mode());
        reduction.setThreshold(form.threshold());
        reduction.setBenefit(form.benefit());
        reduction.setStartTime(form.startTime());
        reduction.setEndTime(form.endTime());
        reduction.setStatus(form.status() == null ? 1 : form.status());
    }
}
