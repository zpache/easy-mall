package com.easymall.admin.modules.dashboard.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.dashboard.service.DashboardService;
import com.easymall.admin.modules.dashboard.vo.DashboardOverviewVO;
import com.easymall.admin.modules.dashboard.vo.MemberGrowthVO;
import com.easymall.admin.modules.dashboard.vo.TopGoodsVO;
import com.easymall.admin.modules.dashboard.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分析页统计接口。
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 顶部 4 个指标卡：今日订单 / 今日销售额 / 待发货订单 / 库存预警 */
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() {
        return Result.ok(dashboardService.overview());
    }

    /** 订单趋势（近 N 天，默认 30，上限 90）：订单数 + 销售额 */
    @GetMapping("/order-trend")
    public Result<List<TrendPointVO>> orderTrend(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(dashboardService.orderTrend(Math.min(Math.max(days, 1), 90)));
    }

    /** 商品销量排行（近 N 天，默认 30，上限 10 条） */
    @GetMapping("/top-goods")
    public Result<List<TopGoodsVO>> topGoods(@RequestParam(defaultValue = "30") int days,
                                             @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(dashboardService.topGoods(
                Math.min(Math.max(days, 1), 90), Math.min(Math.max(limit, 1), 10)));
    }

    /** 会员增长（近 N 天，默认 30）：每日新注册会员数 */
    @GetMapping("/member-growth")
    public Result<List<MemberGrowthVO>> memberGrowth(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(dashboardService.memberGrowth(Math.min(Math.max(days, 1), 90)));
    }
}
