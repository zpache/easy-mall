package cn.easykits.admin.modules.auth.controller;

import cn.easykits.admin.common.api.Result;
import cn.easykits.admin.common.context.LoginUser;
import cn.easykits.admin.common.context.UserContext;
import cn.easykits.admin.modules.auth.dto.LoginRequest;
import cn.easykits.admin.modules.auth.service.AuthService;
import cn.easykits.admin.modules.auth.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 登录认证。
 */
@Tag(name = "系统-登录认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "账号密码登录")
    @SecurityRequirements
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public Result<LoginUser> me() {
        return Result.ok(UserContext.require());
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态：服务端不维护会话，客户端丢弃 token 即可。
        // 若后续需要强制下线，可在这里把 token 的 jti 写入 Redis 黑名单。
        return Result.ok();
    }
}
