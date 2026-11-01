package com.easymall.admin.modules.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.context.LoginUser;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.SysUser;
import com.easymall.admin.mapper.SysUserMapper;
import com.easymall.admin.modules.auth.dto.LoginRequest;
import com.easymall.admin.modules.auth.vo.LoginVO;
import com.easymall.admin.modules.log.service.LoginLogService;
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
 * 管理后台登录：认证 + 防爆破锁定 + 登录日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper sysUserMapper;
    private final LoginGuardService loginGuardService;
    private final LoginLogService loginLogService;

    public LoginVO login(LoginRequest request) {
        // 防爆破：锁定期内直接拒绝，不再触发认证
        loginGuardService.checkLocked(request.username());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (DisabledException e) {
            loginGuardService.recordFailure(request.username());
            loginLogService.record(request.username(), null, false, "账号已停用");
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        } catch (AuthenticationException e) {
            int remain = loginGuardService.recordFailure(request.username());
            loginLogService.record(request.username(), null, false, "账号或密码错误");
            log.warn("登录失败: username={}, reason={}", request.username(), e.getMessage());
            // 接近锁定时提示剩余次数，其余场景统一文案不泄露账号是否存在
            throw new BusinessException(remain > 0 && remain < 3
                    ? "账号或密码错误，还可尝试 " + remain + " 次" : "账号或密码错误");
        }

        AdminUserDetails details = (AdminUserDetails) authentication.getPrincipal();
        LoginUser loginUser = new LoginUser(details.getId(), details.getUsername(),
                details.getNickname(), details.getRoles());
        String token = jwtTokenProvider.createToken(loginUser);

        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, details.getId())
                .set(SysUser::getLastLoginAt, LocalDateTime.now()));

        loginGuardService.recordSuccess(request.username());
        loginLogService.record(request.username(), details.getId(), true, null);

        return new LoginVO(token, token, "Bearer", jwtTokenProvider.getExpireSeconds(), loginUser);
    }
}
