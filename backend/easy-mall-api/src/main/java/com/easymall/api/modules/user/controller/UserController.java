package com.easymall.api.modules.user.controller;

import com.easymall.api.common.api.Result;
import com.easymall.api.entity.AppUser;
import com.easymall.api.modules.user.dto.ProfileForm;
import com.easymall.api.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序用户信息。
 */
@Tag(name = "用户")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "当前用户信息")
    @GetMapping("/profile")
    public Result<AppUser> profile() {
        return Result.ok(userService.currentUser());
    }

    @Operation(summary = "修改用户资料")
    @PutMapping("/profile")
    public Result<AppUser> updateProfile(@Valid @RequestBody ProfileForm form) {
        return Result.ok(userService.updateProfile(form));
    }
}
