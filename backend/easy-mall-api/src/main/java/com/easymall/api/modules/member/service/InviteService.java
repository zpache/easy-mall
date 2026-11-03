package com.easymall.api.modules.member.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.api.entity.AppUser;
import com.easymall.api.entity.MemberInvite;
import com.easymall.api.entity.PointsLog;
import com.easymall.api.mapper.AppUserMapper;
import com.easymall.api.mapper.MemberInviteMapper;
import com.easymall.api.mapper.PointsLogMapper;
import com.easymall.api.modules.setting.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 邀请关系与拉新奖励。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>绑定时机：新用户首次注册（wx.login 自动建号）时，若登录请求带了 inviterId 则建立关系；
 *       一人只能被邀请一次（member_id 唯一），自己邀请自己无效，已注册老用户带 inviter 登录不补绑；</li>
 *   <li>奖励时点：注册成功即奖励邀请人「注册积分」；被邀请人首单支付成功后追加「首单积分」；</li>
 *   <li>幂等：关系行用唯一索引兜底，首单奖励用 status 0→1 的条件更新做闸门，重放/并发不会重复发分；</li>
 *   <li>奖励不阻断主流程：登录和支付链路里奖励失败只记日志。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InviteService {

    private static final String CONFIG_GROUP = "member";
    private static final String KEY_ENABLED = "inviteEnabled";
    private static final String KEY_REGISTER_POINTS = "inviteRegisterPoints";
    private static final String KEY_FIRST_ORDER_POINTS = "inviteFirstOrderPoints";

    /** 积分流水类型：邀请奖励 */
    private static final String TYPE_INVITE = "invite";

    private final MemberInviteMapper inviteMapper;
    private final AppUserMapper appUserMapper;
    private final PointsLogMapper pointsLogMapper;
    private final SysConfigService sysConfigService;

    /**
     * 新用户注册时绑定邀请关系并发注册奖励。
     * 事务挂在外层登录事务里即可：绑定失败只记日志，不影响登录。
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void bindOnRegister(Long inviterId, Long memberId) {
        try {
            if (inviterId == null || inviterId <= 0 || memberId == null) {
                return;
            }
            if (!sysConfigService.getBoolean(CONFIG_GROUP, KEY_ENABLED, true)) {
                return;
            }
            if (inviterId.equals(memberId)) {
                return;
            }
            // 邀请人必须存在且为正常状态
            AppUser inviter = appUserMapper.selectById(inviterId);
            if (inviter == null || !Integer.valueOf(1).equals(inviter.getStatus())) {
                log.warn("邀请绑定跳过：邀请人不存在或被禁用, inviterId={}, memberId={}", inviterId, memberId);
                return;
            }
            int registerPoints = Math.max(sysConfigService.getInt(CONFIG_GROUP, KEY_REGISTER_POINTS, 0), 0);
            int firstOrderPoints = Math.max(sysConfigService.getInt(CONFIG_GROUP, KEY_FIRST_ORDER_POINTS, 0), 0);

            MemberInvite invite = new MemberInvite();
            invite.setMemberId(memberId);
            invite.setInviterId(inviterId);
            invite.setStatus(MemberInvite.STATUS_REGISTERED);
            invite.setRegisterPoints(registerPoints);
            invite.setOrderPoints(firstOrderPoints);
            try {
                inviteMapper.insert(invite);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 该用户已被邀请过（分享链接被多人转发等），保持首次归因
                return;
            }
            if (registerPoints > 0) {
                awardPoints(inviterId, registerPoints, "邀请新用户注册奖励");
            }
            log.info("邀请关系已建立: inviterId={}, memberId={}, registerPoints={}",
                    inviterId, memberId, registerPoints);
        } catch (Exception e) {
            log.warn("邀请绑定失败（不影响登录）: inviterId={}, memberId={}", inviterId, memberId, e);
        }
    }

    /**
     * 被邀请人首单支付成功后追加奖励。
     * 在 MemberPointService.awardForOrder 成功后调用（同事务）。
     */
    public void settleFirstOrder(Long memberId) {
        try {
            if (memberId == null) {
                return;
            }
            MemberInvite invite = inviteMapper.selectOne(Wrappers.<MemberInvite>lambdaQuery()
                    .eq(MemberInvite::getMemberId, memberId)
                    .eq(MemberInvite::getStatus, MemberInvite.STATUS_REGISTERED)
                    .last("LIMIT 1"));
            if (invite == null) {
                return;
            }
            // 幂等闸门：只有把 status 从 0 改成 1 的那次才发奖励
            int updated = inviteMapper.update(null, Wrappers.<MemberInvite>lambdaUpdate()
                    .eq(MemberInvite::getId, invite.getId())
                    .eq(MemberInvite::getStatus, MemberInvite.STATUS_REGISTERED)
                    .set(MemberInvite::getStatus, MemberInvite.STATUS_ORDERED)
                    .set(MemberInvite::getOrderedAt, LocalDateTime.now()));
            if (updated == 0) {
                return;
            }
            if (invite.getOrderPoints() != null && invite.getOrderPoints() > 0) {
                awardPoints(invite.getInviterId(), invite.getOrderPoints(), "被邀请人首单奖励");
            }
            log.info("邀请首单奖励已结算: inviterId={}, memberId={}, points={}",
                    invite.getInviterId(), memberId, invite.getOrderPoints());
        } catch (Exception e) {
            log.warn("邀请首单奖励结算失败（不影响支付主流程）: memberId={}", memberId, e);
        }
    }

    /** 给邀请人加积分并写流水（balance 取累加后的最新值，与消费积分口径一致） */
    private void awardPoints(Long inviterId, int points, String remark) {
        AppUser inviter = appUserMapper.selectById(inviterId);
        if (inviter == null) {
            return;
        }
        appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
                .eq(AppUser::getId, inviterId)
                .setSql("points = points + " + points));
        PointsLog logRecord = new PointsLog();
        logRecord.setUserId(inviterId);
        logRecord.setChange(points);
        logRecord.setBalance((inviter.getPoints() == null ? 0 : inviter.getPoints()) + points);
        logRecord.setType(TYPE_INVITE);
        logRecord.setRemark(remark);
        pointsLogMapper.insert(logRecord);
    }
}
