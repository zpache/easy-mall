package com.easymall.admin.modules.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.AppUser;
import com.easymall.admin.entity.SigninLog;
import com.easymall.admin.entity.SigninRule;
import com.easymall.admin.mapper.AppUserMapper;
import com.easymall.admin.mapper.SigninLogMapper;
import com.easymall.admin.mapper.SigninRuleMapper;
import com.easymall.admin.modules.marketing.dto.SigninRuleForm;
import com.easymall.admin.modules.marketing.vo.SigninLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 签到：规则维护与签到记录查询。
 */
@Service
@RequiredArgsConstructor
public class SigninService extends ServiceImpl<SigninRuleMapper, SigninRule> {

    private final SigninLogMapper signinLogMapper;
    private final AppUserMapper appUserMapper;

    public PageResult<SigninRule> pageRules(long pageNum, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<SigninRule> wrapper = Wrappers.<SigninRule>lambdaQuery()
                .like(StringUtils.hasText(keyword), SigninRule::getName, keyword)
                .eq(status != null, SigninRule::getStatus, status)
                .orderByAsc(SigninRule::getId);
        Page<SigninRule> page = page(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    public SigninRule createRule(SigninRuleForm form) {
        checkBonus(form);
        SigninRule rule = new SigninRule();
        applyForm(rule, form);
        save(rule);
        return rule;
    }

    public SigninRule updateRule(Long id, SigninRuleForm form) {
        SigninRule rule = getById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        checkBonus(form);
        applyForm(rule, form);
        if (form.version() != null) {
            rule.setVersion(form.version());
        }
        if (!updateById(rule)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return rule;
    }

    public void updateRuleStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值只能为 0（停用）或 1（启用）");
        }
        SigninRule rule = getById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        rule.setStatus(status);
        if (!updateById(rule)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    public void deleteRule(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        removeById(id);
    }

    /** 签到记录分页：按会员 / 日期范围过滤。 */
    public PageResult<SigninLogVO> pageLogs(long pageNum, long pageSize, Long userId,
                                            LocalDate beginDate, LocalDate endDate) {
        LambdaQueryWrapper<SigninLog> wrapper = Wrappers.<SigninLog>lambdaQuery()
                .eq(userId != null, SigninLog::getUserId, userId)
                .ge(beginDate != null, SigninLog::getSignDate, beginDate)
                .le(endDate != null, SigninLog::getSignDate, endDate)
                .orderByDesc(SigninLog::getSignDate)
                .orderByDesc(SigninLog::getId);
        Page<SigninLog> page = signinLogMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        List<SigninLog> records = page.getRecords();
        Map<Long, String> nicknames = nicknamesOf(
                records.stream().map(SigninLog::getUserId).collect(Collectors.toSet()));
        List<SigninLogVO> rows = records.stream()
                .map(log -> new SigninLogVO(
                        log.getId(), log.getUserId(), nicknames.get(log.getUserId()),
                        log.getSignDate(), log.getContinuousDays(), log.getPoints(), log.getCreatedAt()))
                .toList();
        return new PageResult<>(rows, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    private void checkBonus(SigninRuleForm form) {
        if (form.bonusEveryDays() != null && form.bonusEveryDays() > 0
                && (form.bonusPoints() == null || form.bonusPoints() <= 0)) {
            throw new BusinessException("启用连续签到奖励时必须填写额外奖励积分");
        }
    }

    private void applyForm(SigninRule rule, SigninRuleForm form) {
        rule.setName(form.name());
        rule.setPointsPerDay(form.pointsPerDay());
        rule.setBonusEveryDays(form.bonusEveryDays() == null ? 0 : form.bonusEveryDays());
        rule.setBonusPoints(form.bonusPoints() == null ? 0 : form.bonusPoints());
        rule.setStatus(form.status() == null ? 1 : form.status());
        rule.setRemark(form.remark());
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
