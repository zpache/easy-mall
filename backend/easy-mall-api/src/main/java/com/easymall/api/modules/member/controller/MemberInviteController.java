package com.easymall.api.modules.member.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.api.common.api.PageResult;
import com.easymall.api.common.api.Result;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.entity.AppUser;
import com.easymall.api.entity.MemberInvite;
import com.easymall.api.mapper.AppUserMapper;
import com.easymall.api.mapper.MemberInviteMapper;
import com.easymall.api.modules.member.vo.InviteRecordVO;
import com.easymall.api.modules.member.vo.InviteSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 我的邀请：邀请人数、累计奖励积分与邀请记录。
 */
@Tag(name = "会员-邀请")
@RestController
@RequestMapping("/member/invite")
@RequiredArgsConstructor
public class MemberInviteController {

    private final MemberInviteMapper inviteMapper;
    private final AppUserMapper appUserMapper;

    @Operation(summary = "我的邀请汇总 + 邀请记录分页")
    @GetMapping("/records")
    public Result<InviteSummaryVO> records(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        Long userId = UserContext.userId();

        List<MemberInvite> all = inviteMapper.selectList(Wrappers.<MemberInvite>lambdaQuery()
                .eq(MemberInvite::getInviterId, userId)
                .orderByDesc(MemberInvite::getId));
        int rewardPoints = all.stream()
                .mapToInt(item -> (item.getRegisterPoints() == null ? 0 : item.getRegisterPoints())
                        + (item.getOrderPoints() == null ? 0 : item.getOrderPoints()))
                .sum();

        // 手工分页：邀请记录通常不多，一次取回后再切页，省一次 count + join
        int from = (int) Math.min((pageNum - 1) * pageSize, all.size());
        int to = (int) Math.min(from + pageSize, all.size());
        List<MemberInvite> pageItems = all.subList(from, to);

        Map<Long, AppUser> users = loadUsers(pageItems);
        List<InviteRecordVO> records = pageItems.stream()
                .map(item -> InviteRecordVO.of(item, users.get(item.getMemberId())))
                .toList();
        return Result.ok(new InviteSummaryVO(all.size(), rewardPoints,
                new PageResult<>(records, all.size(), pageNum, pageSize,
                        pageSize == 0 ? 0 : (all.size() + pageSize - 1) / pageSize)));
    }

    private Map<Long, AppUser> loadUsers(List<MemberInvite> items) {
        if (items.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = items.stream().map(MemberInvite::getMemberId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return appUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity()));
    }
}
