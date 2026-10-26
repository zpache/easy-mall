package com.easymall.api.common.context;

import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.exception.BusinessException;

/**
 * 当前请求的登录上下文。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser loginUser = HOLDER.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long userId() {
        return require().userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
