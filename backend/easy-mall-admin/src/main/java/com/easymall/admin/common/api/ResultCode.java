package com.easymall.admin.common.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 业务响应码。
 * 成功码为 0（vben 前端约定）；框架级错误码沿用 HTTP 状态语义，业务错误从 4xxx 起。
 */
@Getter
@RequiredArgsConstructor
public enum ResultCode {

    SUCCESS(0, "操作成功"),
    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "登录已失效，请重新登录"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    INTERNAL_ERROR(500, "系统繁忙，请稍后再试"),

    /* 业务类错误码：4xxx 起，按模块分段 */
    ACCOUNT_NOT_FOUND(4001, "账号不存在"),
    ACCOUNT_PASSWORD_ERROR(4002, "账号或密码错误"),
    ACCOUNT_DISABLED(4003, "账号已被禁用"),
    ACCOUNT_EXISTS(4004, "账号已存在"),
    DATA_NOT_FOUND(4005, "数据不存在或已被删除"),
    DATA_CONFLICT(4006, "数据状态冲突，操作失败");

    private final int code;
    private final String message;
}
