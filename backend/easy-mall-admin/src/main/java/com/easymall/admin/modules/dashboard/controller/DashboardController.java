package com.easymall.admin.modules.dashboard.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.dashboard.service.DashboardService;
import com.easymall.admin.modules.dashboard.vo.DashboardOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
