package com.easymall.api.modules.user.service;

import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.entity.AppUser;
import com.easymall.api.entity.MemberLevel;
import com.easymall.api.mapper.AppUserMapper;
import com.easymall.api.mapper.MemberLevelMapper;
import com.easymall.api.modules.user.dto.ProfileForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 小程序用户服务。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserMapper appUserMapper;
    private final MemberLevelMapper memberLevelMapper;

    public AppUser currentUser() {
        AppUser user = appUserMapper.selectById(UserContext.userId());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        fillLevelName(user);
        return user;
    }

    public AppUser updateProfile(ProfileForm form) {
        AppUser user = currentUser();
        if (StringUtils.hasText(form.nickname())) {
            user.setNickname(form.nickname().trim());
        }
        if (StringUtils.hasText(form.avatar())) {
            String avatar = form.avatar().trim();
            // 只接受 http(s) 地址或本服务上传路径，避免把 javascript: / 本地路径写进头像字段
            if (!(avatar.startsWith("https://") || avatar.startsWith("http://") || avatar.startsWith("/files/"))) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "头像地址不合法");
            }
            user.setAvatar(avatar);
        }
        if (form.gender() != null) {
            user.setGender(form.gender());
        }
        if (appUserMapper.updateById(user) == 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT);
        }
        return user;
    }

    /** 等级名称与折扣仅用于展示，避免小程序为了一个名字再查一次等级表 */
    private void fillLevelName(AppUser user) {
        if (user.getLevelId() == null) {
            return;
        }
        MemberLevel level = memberLevelMapper.selectById(user.getLevelId());
        if (level != null) {
            user.setLevelName(level.getName());
            // 折扣按百分比下发，前端用同一公式本地估算结算页的会员优惠金额
            user.setLevelDiscount(level.getDiscount());
        }
    }
}
