package com.easymall.admin.modules.member.vo;

/**
 * 会员等级折扣总开关。
 */
public record LevelDiscountSwitchVO(

        /** 是否开启会员等级折扣 */
        Boolean enabled,

        /** 启用中且折扣率低于 100（即真正有折扣）的等级数量，用于提示「关掉会影响 N 个等级」 */
        Integer discountLevelCount
) {
}
