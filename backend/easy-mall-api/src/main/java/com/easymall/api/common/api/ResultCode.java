package com.easymall.api.common.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 业务响应码。成功码为 0（前端约定），业务错误从 4xxx 起。
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

    /* 业务类错误码 */
    GOODS_NOT_FOUND(4001, "商品不存在或已下架"),
    GOODS_OFF_SHELF(4002, "商品已下架"),
    STOCK_NOT_ENOUGH(4003, "商品库存不足"),
    CART_EMPTY(4004, "购物车中没有选中的商品"),
    ADDRESS_NOT_FOUND(4005, "收货地址不存在"),
    ORDER_NOT_FOUND(4006, "订单不存在"),
    ORDER_STATUS_ERROR(4007, "订单状态不支持该操作"),
    WX_LOGIN_FAILED(4008, "微信登录失败，请重试"),
    USER_DISABLED(4009, "账号已被禁用"),
    DATA_CONFLICT(4010, "数据已被修改，请刷新后重试"),

    COUPON_NOT_FOUND(4011, "优惠券不存在或已下架"),
    COUPON_OFFLINE(4012, "优惠券已停止发放"),
    COUPON_SOLD_OUT(4013, "优惠券已被领完"),
    COUPON_EXPIRED(4014, "优惠券已过领取时间"),
    COUPON_LIMIT_REACHED(4015, "已达到每人限领张数"),

    FILE_EMPTY(4016, "请选择要上传的文件"),
    FILE_TOO_LARGE(4017, "图片体积过大，请压缩后重试"),
    FILE_TYPE_NOT_ALLOWED(4018, "仅支持 jpg/png/gif/webp 图片"),
    FILE_SAVE_FAILED(4019, "文件保存失败，请稍后重试"),

    REQUEST_BUSY(4020, "操作过于频繁，请稍后重试"),

    COUPON_NOT_USABLE(4021, "优惠券不可用，请重新选择"),

    FREIGHT_NOT_DELIVERABLE(4022, "该地区暂不提供配送"),

    PAY_CHANNEL_CLOSED(4023, "支付渠道未开启，请联系客服"),

    PAY_FAILED(4024, "拉起支付失败，请稍后重试"),
    PAY_NOT_CONFIGURED(4025, "微信支付参数未配置完整"),
    PAY_NOTIFY_INVALID(4026, "支付回调校验失败");

    private final int code;
    private final String message;
}
