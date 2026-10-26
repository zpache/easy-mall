package com.easymall.admin.common.context;

import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;

/**
 * 当前请求的登录上下文（ThreadLocal 实现）。
 * 使用方在请求线程内通过 {@link #get()} 获取登录主体。
 * 过滤器在 finally 中调用 {@link #clear()}，避免线程复用导致的数据串号。
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

    /**
     * 获取登录主体，未登录时抛出 UNAUTHORIZED 异常。
     */
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
