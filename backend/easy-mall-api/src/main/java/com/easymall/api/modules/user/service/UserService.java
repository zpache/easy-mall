package com.easymall.api.modules.user.service;

import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.entity.AppUser;
import com.easymall.api.mapper.AppUserMapper;
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

    public AppUser currentUser() {
        AppUser user = appUserMapper.selectById(UserContext.userId());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
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
}
