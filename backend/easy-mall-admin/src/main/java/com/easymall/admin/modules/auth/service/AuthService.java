package com.easymall.admin.modules.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.context.LoginUser;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.SysUser;
import com.easymall.admin.mapper.SysUserMapper;
import com.easymall.admin.modules.auth.dto.LoginRequest;
import com.easymall.admin.modules.auth.vo.LoginVO;
import com.easymall.admin.security.AdminUserDetails;
import com.easymall.admin.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理后台登录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper sysUserMapper;

    public LoginVO login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (DisabledException e) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        } catch (AuthenticationException e) {
            log.warn("登录失败: username={}, reason={}", request.username(), e.getMessage());
            throw new BusinessException(ResultCode.ACCOUNT_PASSWORD_ERROR);
        }

        AdminUserDetails details = (AdminUserDetails) authentication.getPrincipal();
        LoginUser loginUser = new LoginUser(details.getId(), details.getUsername(),
                details.getNickname(), details.getRole());
        String token = jwtTokenProvider.createToken(loginUser);

        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, details.getId())
                .set(SysUser::getLastLoginAt, LocalDateTime.now()));

        return new LoginVO(token, "Bearer", jwtTokenProvider.getExpireSeconds(), loginUser);
    }
}
