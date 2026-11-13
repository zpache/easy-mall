package cn.easykits.api.modules.member.vo;

import cn.easykits.api.common.api.PageResult;

/**
 * 我的邀请汇总。
 *
 * @param invitedCount 累计邀请人数
 * @param rewardPoints 累计获得的邀请奖励积分（注册 + 首单）
 * @param records      邀请记录分页
 */
public record InviteSummaryVO(long invitedCount, int rewardPoints, PageResult<InviteRecordVO> records) {
}
