package cn.easykits.api.modules.decorate.controller;

import cn.easykits.api.common.api.Result;
import cn.easykits.api.modules.coupon.service.MemberCouponService;
import cn.easykits.api.modules.coupon.vo.CouponCardVO;
import cn.easykits.api.modules.decorate.service.DecorateService;
import cn.easykits.api.modules.decorate.vo.DecoratePageVO;
import cn.easykits.api.modules.decorate.vo.NavConfigVO;
import cn.easykits.api.modules.decorate.vo.ThemeConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城装修（免登录）：首页与微页面结构、全局导航配置、装修组件的券卡片。
 */
@Tag(name = "装修")
@RestController
@RequestMapping("/decorate")
@RequiredArgsConstructor
public class DecorateController {

    private final DecorateService decorateService;
    private final MemberCouponService memberCouponService;

    @Operation(summary = "商城首页（含导航配置）")
    @GetMapping("/home")
    public Result<DecoratePageVO> home() {
        return Result.ok(decorateService.home());
    }

    @Operation(summary = "微页面（含导航配置）")
    @GetMapping("/page/{id}")
    public Result<DecoratePageVO> page(@PathVariable Long id) {
        return Result.ok(decorateService.page(id));
    }

    @Operation(summary = "全局导航配置（TabBar / 顶部导航 / 悬浮球）")
    @GetMapping("/nav")
    public Result<NavConfigVO> nav() {
        return Result.ok(decorateService.navConfig());
    }

    @Operation(summary = "线上主题配置（换肤，无启用主题时为空）")
    @GetMapping("/theme")
    public Result<ThemeConfigVO> theme(HttpServletResponse response) {
        // 主题会被后台随时切换，禁止客户端与网关缓存，保证小程序端拿到的是最新配色
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        return Result.ok(decorateService.themeConfig());
    }

    @Operation(summary = "装修「优惠券」组件的券卡片（按传入随机码顺序返回）")
    @GetMapping("/coupons")
    public Result<List<CouponCardVO>> coupons(@RequestParam(required = false) List<String> codes) {
        return Result.ok(memberCouponService.cards(codes));
    }
}
