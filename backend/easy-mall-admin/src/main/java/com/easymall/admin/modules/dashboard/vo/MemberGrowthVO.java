package com.easymall.admin.modules.dashboard.vo;

/**
 * 会员增长单点：某日新注册会员数。
 */
public record MemberGrowthVO(String date, long newMembers) {
}
