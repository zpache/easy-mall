package com.easymall.admin.modules.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.entity.AppUser;
import com.easymall.admin.entity.PointsLog;
import com.easymall.admin.mapper.AppUserMapper;
import com.easymall.admin.mapper.PointsLogMapper;
import com.easymall.admin.modules.member.vo.PointsLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 会员积分：流水记录与分页查询。
 */
@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsLogMapper pointsLogMapper;
    private final AppUserMapper appUserMapper;

    /**
     * 写一条积分流水（人工调整时操作人取登录上下文）。
     */
    public void record(Long userId, int change, int balance, String type, String orderNo, String remark) {
        PointsLog log = new PointsLog();
        log.setUserId(userId);
        log.setChange(change);
        log.setBalance(balance);
        log.setType(type);
        log.setOrderNo(orderNo);
        log.setRemark(remark);
        log.setCreatedBy(com.easymall.admin.common.context.UserContext.get() == null
                ? null : com.easymall.admin.common.context.UserContext.get().userId());
        pointsLogMapper.insert(log);
    }

    public PageResult<PointsLogVO> pageLogs(long pageNum, long pageSize, Long userId, String type) {
        LambdaQueryWrapper<PointsLog> wrapper = Wrappers.<PointsLog>lambdaQuery()
                .eq(userId != null, PointsLog::getUserId, userId)
                .eq(StringUtils.hasText(type), PointsLog::getType, type)
                .orderByDesc(PointsLog::getId);
        Page<PointsLog> page = pointsLogMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        Map<Long, String> nicknames = nicknamesOf(
                page.getRecords().stream().map(PointsLog::getUserId).collect(Collectors.toSet()));
        List<PointsLogVO> records = page.getRecords().stream()
                .map(log -> new PointsLogVO(
                        log.getId(), log.getUserId(), nicknames.get(log.getUserId()),
                        log.getChange(), log.getBalance(), log.getType(),
                        log.getOrderNo(), log.getRemark(), log.getCreatedAt()))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
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
