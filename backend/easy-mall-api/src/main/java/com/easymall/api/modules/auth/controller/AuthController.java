package com.easymall.api.modules.auth.controller;

import com.easymall.api.common.api.Result;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.modules.auth.dto.WxLoginRequest;
import com.easymall.api.modules.auth.service.WxAuthService;
import com.easymall.api.modules.auth.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    private final WxAuthService wxAuthService;

    @Operation(summary = "微信登录（code 换 token）")
    @SecurityRequirements
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody WxLoginRequest request) {
        return Result.ok(wxAuthService.login(request.code()));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态，客户端清除本地 token 即可
        UserContext.clear();
        return Result.ok();
    }
}
