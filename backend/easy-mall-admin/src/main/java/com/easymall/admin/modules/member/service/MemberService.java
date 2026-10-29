package com.easymall.admin.modules.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.AppUser;
import com.easymall.admin.entity.MemberLevel;
import com.easymall.admin.entity.MemberTag;
import com.easymall.admin.entity.MemberUserTag;
import com.easymall.admin.mapper.AppUserMapper;
import com.easymall.admin.mapper.MemberLevelMapper;
import com.easymall.admin.mapper.MemberTagMapper;
import com.easymall.admin.mapper.MemberUserTagMapper;
import com.easymall.admin.modules.member.dto.MemberTagForm;
import com.easymall.admin.modules.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 会员服务：列表筛选（关键词 / 等级 / 来源渠道 / 标签）、详情、打标签、积分调整。
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final AppUserMapper appUserMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final MemberTagMapper memberTagMapper;
    private final MemberUserTagMapper memberUserTagMapper;
    private final PointsService pointsService;

    public PageResult<MemberVO> pageQuery(long pageNum, long pageSize, String keyword,
                                          Long levelId, Integer source, Long tagId) {
        // 标签筛选：先取该标签下的会员 ID 集合
        List<Long> tagUserIds = null;
        if (tagId != null) {
            tagUserIds = memberUserTagMapper.selectList(Wrappers.<MemberUserTag>lambdaQuery()
                            .eq(MemberUserTag::getTagId, tagId))
                    .stream().map(MemberUserTag::getUserId).distinct().toList();
            if (tagUserIds.isEmpty()) {
                return PageResult.empty(pageNum, pageSize);
            }
        }

        LambdaQueryWrapper<AppUser> wrapper = Wrappers.<AppUser>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(AppUser::getNickname, keyword)
                        .or().like(AppUser::getPhone, keyword))
                .eq(levelId != null, AppUser::getLevelId, levelId)
                .eq(source != null, AppUser::getSource, source)
                .in(tagUserIds != null, AppUser::getId, tagUserIds)
                .orderByDesc(AppUser::getId);
        Page<AppUser> page = appUserMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        List<MemberVO> records = toVOs(page.getRecords());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public MemberVO detail(Long id) {
        AppUser user = appUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return toVOs(List.of(user)).getFirst();
    }

    /**
     * 打标签：全量覆盖该会员的标签关联。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTags(Long userId, MemberTagForm form) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        List<Long> tagIds = form.tagIds().stream().distinct().toList();
        if (!tagIds.isEmpty()) {
            long exists = memberTagMapper.selectCount(Wrappers.<MemberTag>lambdaQuery()
                    .in(MemberTag::getId, tagIds));
            if (exists != tagIds.size()) {
                throw new BusinessException("存在无效的标签，请刷新后重试");
            }
        }
        memberUserTagMapper.delete(Wrappers.<MemberUserTag>lambdaQuery()
                .eq(MemberUserTag::getUserId, userId));
        for (Long tagId : tagIds) {
            MemberUserTag relation = new MemberUserTag();
            relation.setUserId(userId);
            relation.setTagId(tagId);
            memberUserTagMapper.insert(relation);
        }
    }

    /**
     * 会员积分人工调整：更新余额并写流水。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjustPoints(Long userId, Integer change, String remark) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        int balance = user.getPoints() == null ? 0 : user.getPoints();
        if (balance + change < 0) {
            throw new BusinessException("积分不足，当前余额 " + balance + " 分");
        }
        user.setPoints(balance + change);
        if (appUserMapper.updateById(user) == 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        pointsService.record(userId, change, balance + change, "adjust", null, remark);
    }

    // ------------------------------------------------------------
    // 私有：实体聚合为 VO
    // ------------------------------------------------------------

    private List<MemberVO> toVOs(List<AppUser> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        Map<Long, MemberLevel> levels = levelsOf(users);
        Map<Long, List<MemberVO.TagItem>> tagMap = tagsOf(users);
        return users.stream().map(user -> new MemberVO(
                user.getId(),
                user.getOpenid(),
                user.getNickname(),
                user.getAvatar(),
                user.getPhone(),
                user.getGender(),
                user.getStatus(),
                user.getLevelId(),
                levels.containsKey(user.getLevelId()) ? levels.get(user.getLevelId()).getName() : null,
                levels.containsKey(user.getLevelId()) ? levels.get(user.getLevelId()).getDiscount() : null,
                user.getPoints(),
                user.getSource(),
                user.getTotalSpend(),
                user.getOrderCount(),
                tagMap.getOrDefault(user.getId(), List.of()),
                user.getCreatedAt()
        )).toList();
    }

    private Map<Long, MemberLevel> levelsOf(List<AppUser> users) {
        List<Long> levelIds = users.stream()
                .map(AppUser::getLevelId)
                .filter(java.util.Objects::nonNull)
                .distinct().toList();
        if (levelIds.isEmpty()) {
            return Map.of();
        }
        return memberLevelMapper.selectBatchIds(levelIds).stream()
                .collect(Collectors.toMap(MemberLevel::getId, Function.identity()));
    }

    private Map<Long, List<MemberVO.TagItem>> tagsOf(List<AppUser> users) {
        List<Long> userIds = users.stream().map(AppUser::getId).toList();
        List<MemberUserTag> relations = memberUserTagMapper.selectList(Wrappers.<MemberUserTag>lambdaQuery()
                .in(MemberUserTag::getUserId, userIds));
        if (relations.isEmpty()) {
            return Map.of();
        }
        List<Long> tagIds = relations.stream().map(MemberUserTag::getTagId).distinct().toList();
        Map<Long, MemberTag> tags = memberTagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(MemberTag::getId, Function.identity()));
        return relations.stream()
                .collect(Collectors.groupingBy(MemberUserTag::getUserId,
                        Collectors.mapping(relation -> {
                            MemberTag tag = tags.get(relation.getTagId());
                            return tag == null ? null
                                    : new MemberVO.TagItem(tag.getId(), tag.getName(), tag.getColor());
                        }, Collectors.toList())))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().filter(java.util.Objects::nonNull).toList()));
    }

    /**
     * 批量取会员昵称（积分流水等场景用）。
     */
    public Map<Long, String> nicknamesOf(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return appUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(AppUser::getId,
                        user -> user.getNickname() == null ? "会员" + user.getId() : user.getNickname()));
    }
}
