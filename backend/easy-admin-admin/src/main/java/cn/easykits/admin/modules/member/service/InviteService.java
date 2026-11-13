package cn.easykits.admin.modules.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.easykits.admin.common.api.PageResult;
import cn.easykits.admin.entity.AppUser;
import cn.easykits.admin.entity.MemberInvite;
import cn.easykits.admin.mapper.AppUserMapper;
import cn.easykits.admin.mapper.MemberInviteMapper;
import cn.easykits.admin.modules.member.vo.InviteRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 邀请记录：后台只读查询（状态 / 关键词筛选）。
 */
@Service
@RequiredArgsConstructor
public class InviteService {

    private final MemberInviteMapper inviteMapper;
    private final AppUserMapper appUserMapper;

    public PageResult<InviteRecordVO> pageQuery(long pageNum, long pageSize, Integer status, String keyword) {
        LambdaQueryWrapper<MemberInvite> wrapper = Wrappers.<MemberInvite>lambdaQuery()
                .eq(status != null, MemberInvite::getStatus, status)
                .orderByDesc(MemberInvite::getId);

        if (StringUtils.hasText(keyword)) {
            // 关键词按会员昵称 / 手机号 / ID 命中后，再反查牵涉到的邀请关系
            List<Long> matchedUserIds = appUserMapper.selectList(Wrappers.<AppUser>lambdaQuery()
                            .and(w -> w.like(AppUser::getNickname, keyword)
                                    .or().like(AppUser::getPhone, keyword))
                            .or().eq(AppUser::getId, parseId(keyword)))
                    .stream().map(AppUser::getId).toList();
            if (matchedUserIds.isEmpty()) {
                return PageResult.empty(pageNum, pageSize);
            }
            wrapper.and(w -> w.in(MemberInvite::getMemberId, matchedUserIds)
                    .or().in(MemberInvite::getInviterId, matchedUserIds));
        }

        Page<MemberInvite> page = inviteMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        Map<Long, AppUser> users = loadUsers(page.getRecords());
        List<InviteRecordVO> records = page.getRecords().stream()
                .map(item -> toVO(item, users))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    private Long parseId(String keyword) {
        try {
            return Long.parseLong(keyword.trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private Map<Long, AppUser> loadUsers(Collection<MemberInvite> items) {
        List<Long> ids = items.stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getMemberId(), item.getInviterId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return appUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity()));
    }

    private InviteRecordVO toVO(MemberInvite item, Map<Long, AppUser> users) {
        AppUser member = users.get(item.getMemberId());
        AppUser inviter = users.get(item.getInviterId());
        return new InviteRecordVO(
                item.getId(),
                item.getMemberId(),
                member == null ? "已注销" : member.getNickname(),
                item.getInviterId(),
                inviter == null ? "已注销" : inviter.getNickname(),
                item.getStatus(),
                item.getRegisterPoints(),
                item.getOrderPoints(),
                item.getCreatedAt(),
                item.getOrderedAt()
        );
    }
}
