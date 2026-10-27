package com.easymall.admin.modules.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.AppUser;
import com.easymall.admin.entity.MemberLevel;
import com.easymall.admin.mapper.AppUserMapper;
import com.easymall.admin.mapper.MemberLevelMapper;
import com.easymall.admin.modules.member.dto.LevelForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 会员等级与权益。
 */
@Service
@RequiredArgsConstructor
public class LevelService extends com.baomidou.mybatisplus.spring.service.impl.ServiceImpl<MemberLevelMapper, MemberLevel> {

    private final AppUserMapper appUserMapper;

    public PageResult<MemberLevel> pageQuery(long pageNum, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<MemberLevel> wrapper = Wrappers.<MemberLevel>lambdaQuery()
                .like(StringUtils.hasText(keyword), MemberLevel::getName, keyword)
                .eq(status != null, MemberLevel::getStatus, status)
                .orderByAsc(MemberLevel::getSort)
                .orderByAsc(MemberLevel::getId);
        Page<MemberLevel> page = page(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    /** 启用中的等级（下拉选项用）。 */
    public List<MemberLevel> listEnabled() {
        return list(Wrappers.<MemberLevel>lambdaQuery()
                .eq(MemberLevel::getStatus, 1)
                .orderByAsc(MemberLevel::getSort)
                .orderByAsc(MemberLevel::getId));
    }

    public MemberLevel create(LevelForm form) {
        checkName(form.name(), null);
        MemberLevel level = new MemberLevel();
        applyForm(level, form);
        save(level);
        return level;
    }

    public MemberLevel update(Long id, LevelForm form) {
        MemberLevel level = getById(id);
        if (level == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        checkName(form.name(), id);
        applyForm(level, form);
        if (form.version() != null) {
            level.setVersion(form.version());
        }
        if (!updateById(level)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return level;
    }

    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值只能为 0（停用）或 1（启用）");
        }
        MemberLevel level = getById(id);
        if (level == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        level.setStatus(status);
        if (!updateById(level)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    public void delete(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        long used = appUserMapper.selectCount(Wrappers.<AppUser>lambdaQuery().eq(AppUser::getLevelId, id));
        if (used > 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT,
                    "该等级下还有 " + used + " 位会员，请先调整会员等级");
        }
        removeById(id);
    }

    private void checkName(String name, Long excludeId) {
        long exists = count(Wrappers.<MemberLevel>lambdaQuery()
                .eq(MemberLevel::getName, name)
                .ne(excludeId != null, MemberLevel::getId, excludeId));
        if (exists > 0) {
            throw new BusinessException("等级名称已存在");
        }
    }

    private void applyForm(MemberLevel level, LevelForm form) {
        level.setName(form.name());
        level.setDiscount(form.discount());
        level.setThreshold(form.threshold());
        level.setDescription(form.description());
        level.setSort(form.sort() == null ? 0 : form.sort());
        level.setStatus(form.status() == null ? 1 : form.status());
    }
}
