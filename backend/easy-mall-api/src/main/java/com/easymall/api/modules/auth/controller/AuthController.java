package com.easymall.api.modules.auth.controller;

import com.easymall.api.common.api.Result;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.modules.auth.dto.WxLoginRequest;
import com.easymall.api.modules.auth.service.WxAuthService;
import com.easymall.api.modules.auth.vo.LoginVO;
import com.easymall.api.security.LoginSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序登录。
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final WxAuthService wxAuthService;
    private final LoginSessionService loginSessionService;

    @Operation(summary = "微信登录（code 换自定义登录态）")
    @SecurityRequirements
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody WxLoginRequest request) {
        return Result.ok(wxAuthService.login(request.code()));
    }

    @Operation(summary = "退出登录（服务端销毁登录态）")
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        loginSessionService.remove(stripBearer(authorization));
        UserContext.clear();
        return Result.ok();
    }

    private String stripBearer(String header) {
        return StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)
                ? header.substring(BEARER_PREFIX.length()).trim()
                : null;
    }
}
