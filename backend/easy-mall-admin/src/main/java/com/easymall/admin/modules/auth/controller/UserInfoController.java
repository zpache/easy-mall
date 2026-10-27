package com.easymall.admin.modules.auth.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.common.context.LoginUser;
import com.easymall.admin.common.context.UserContext;
import com.easymall.admin.entity.SysUser;
import com.easymall.admin.mapper.SysUserMapper;
import com.easymall.admin.modules.auth.vo.UserInfoVO;
import com.easymall.admin.modules.system.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * vben 前端约定的用户信息接口。
 * 前端框架在刷新页面 / 换取路由权限时会调用这两个接口。
 */
@Tag(name = "系统-当前用户信息")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserInfoController {

    private final SysUserMapper sysUserMapper;
    private final MenuService menuService;

    @Operation(summary = "当前用户信息")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        LoginUser loginUser = UserContext.require();
        // avatar 不在 JWT 里，回库补一次
        SysUser sysUser = sysUserMapper.selectById(loginUser.userId());
        return Result.ok(new UserInfoVO(
                loginUser.userId(),
                loginUser.username(),
                loginUser.nickname(),
                sysUser == null ? null : sysUser.getAvatar(),
                loginUser.roles(),
                menuService.resolveHomePath(loginUser.userId())));
    }

    @Operation(summary = "当前用户权限码")
    @GetMapping("/codes")
    public Result<List<String>> codes() {
        LoginUser loginUser = UserContext.require();
        return Result.ok(menuService.listAuthCodesByUser(loginUser.userId()));
    }
}
