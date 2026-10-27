package com.easymall.admin.modules.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.MemberTag;
import com.easymall.admin.entity.MemberUserTag;
import com.easymall.admin.mapper.MemberTagMapper;
import com.easymall.admin.mapper.MemberUserTagMapper;
import com.easymall.admin.modules.member.dto.TagForm;
import com.easymall.admin.modules.member.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 会员标签与分群。
 */
@Service
@RequiredArgsConstructor
public class TagService extends com.baomidou.mybatisplus.spring.service.impl.ServiceImpl<MemberTagMapper, MemberTag> {

    private final MemberUserTagMapper memberUserTagMapper;

    public PageResult<TagVO> pageQuery(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<MemberTag> wrapper = Wrappers.<MemberTag>lambdaQuery()
                .like(StringUtils.hasText(keyword), MemberTag::getName, keyword)
                .orderByAsc(MemberTag::getId);
        Page<MemberTag> page = page(Page.of(pageNum, pageSize), wrapper);
        Map<Long, Long> counts = countByTagIds(page.getRecords().stream().map(MemberTag::getId).toList());
        List<TagVO> records = page.getRecords().stream()
                .map(tag -> new TagVO(tag.getId(), tag.getName(), tag.getColor(), tag.getRemark(),
                        counts.getOrDefault(tag.getId(), 0L), tag.getCreatedAt()))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /** 全部标签（会员列表打标下拉用）。 */
    public List<TagVO> listAll() {
        List<MemberTag> tags = list(Wrappers.<MemberTag>lambdaQuery().orderByAsc(MemberTag::getId));
        Map<Long, Long> counts = countByTagIds(tags.stream().map(MemberTag::getId).toList());
        return tags.stream()
                .map(tag -> new TagVO(tag.getId(), tag.getName(), tag.getColor(), tag.getRemark(),
                        counts.getOrDefault(tag.getId(), 0L), tag.getCreatedAt()))
                .toList();
    }

    public MemberTag create(TagForm form) {
        checkName(form.name(), null);
        MemberTag tag = new MemberTag();
        applyForm(tag, form);
        save(tag);
        return tag;
    }

    public MemberTag update(Long id, TagForm form) {
        MemberTag tag = getById(id);
        if (tag == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        checkName(form.name(), id);
        applyForm(tag, form);
        if (form.version() != null) {
            tag.setVersion(form.version());
        }
        if (!updateById(tag)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return tag;
    }

    /** 删除标签并清理关联，实现「取消分群」。 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        memberUserTagMapper.delete(Wrappers.<MemberUserTag>lambdaQuery().eq(MemberUserTag::getTagId, id));
        removeById(id);
    }

    private Map<Long, Long> countByTagIds(List<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return Map.of();
        }
        return memberUserTagMapper.selectList(Wrappers.<MemberUserTag>lambdaQuery()
                        .in(MemberUserTag::getTagId, tagIds)).stream()
                .collect(Collectors.groupingBy(MemberUserTag::getTagId, Collectors.counting()));
    }

    private void checkName(String name, Long excludeId) {
        long exists = count(Wrappers.<MemberTag>lambdaQuery()
                .eq(MemberTag::getName, name)
                .ne(excludeId != null, MemberTag::getId, excludeId));
        if (exists > 0) {
            throw new BusinessException("标签名称已存在");
        }
    }

    private void applyForm(MemberTag tag, TagForm form) {
        tag.setName(form.name());
        tag.setColor(form.color());
        tag.setRemark(form.remark());
    }
}
