package com.easymall.api.modules.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.context.LoginUser;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.entity.AppUser;
import com.easymall.api.mapper.AppUserMapper;
import com.easymall.api.modules.auth.config.WxProperties;
import com.easymall.api.modules.auth.dto.WxSessionResponse;
import com.easymall.api.modules.auth.vo.LoginVO;
import com.easymall.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 微信小程序登录：code -> openid -> 本地用户（不存在则自动注册）-> JWT。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxAuthService {

    private final WxProperties wxProperties;
    private final AppUserMapper appUserMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestClient restClient = RestClient.create();

    public LoginVO login(String code) {
        String openid = resolveOpenid(code);

        AppUser user = appUserMapper.selectOne(
                Wrappers.<AppUser>lambdaQuery().eq(AppUser::getOpenid, openid));
        if (user == null) {
            user = new AppUser();
            user.setOpenid(openid);
            user.setNickname("微信用户");
            user.setGender(0);
            user.setStatus(1);
            appUserMapper.insert(user);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        LoginUser loginUser = new LoginUser(user.getId(), user.getOpenid(), user.getNickname());
        return new LoginVO(
                jwtTokenProvider.createToken(loginUser),
                "Bearer",
                jwtTokenProvider.getExpireSeconds(),
                loginUser);
    }

    private String resolveOpenid(String code) {
        if (wxProperties.isMock()) {
            log.warn("easy-mall.wx.mock=true，使用模拟 openid，请勿在生产环境开启");
            return "mock_" + code;
        }
        if (!StringUtils.hasText(wxProperties.getAppId()) || !StringUtils.hasText(wxProperties.getAppSecret())) {
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED, "未配置小程序 appId/appSecret");
        }
        WxSessionResponse response;
        try {
            response = restClient.get()
                    .uri(wxProperties.getSessionUrl()
                                    + "?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code",
                            wxProperties.getAppId(), wxProperties.getAppSecret(), code)
                    .retrieve()
                    .body(WxSessionResponse.class);
        } catch (Exception e) {
            log.error("调用微信 jscode2session 失败", e);
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED);
        }
        if (response == null || !response.success()) {
            log.warn("微信登录失败: errcode={}, errmsg={}",
                    response == null ? null : response.errcode(),
                    response == null ? null : response.errmsg());
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED);
        }
        return response.openid();
    }
}
