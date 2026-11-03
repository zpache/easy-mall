package com.easymall.api.modules.share.controller;

import com.easymall.api.common.api.Result;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.modules.share.service.WxaCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分享相关：商品小程序码（分享海报用）。
 */
@Tag(name = "分享")
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {

    private final WxaCodeService wxaCodeService;

    @Operation(summary = "商品小程序码（scene 带 id 与 inv 邀请人）")
    @GetMapping("/qrcode/goods/{goodsId}")
    public Result<String> goodsQrcode(@PathVariable long goodsId) {
        // 允许匿名调用：海报是给所有浏览者用的，未登录时 scene 只带商品 id
        Long inviterId = null;
        try {
            inviterId = UserContext.userId();
        } catch (Exception ignored) {
            // 未登录场景下生成不带邀请人的码
        }
        return Result.ok(wxaCodeService.goodsQrcode(goodsId, inviterId));
    }
}
