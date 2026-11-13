package cn.easykits.admin.modules.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.easykits.admin.entity.Goods;
import cn.easykits.admin.entity.Order;
import cn.easykits.admin.mapper.GoodsMapper;
import cn.easykits.admin.mapper.OrderMapper;
import cn.easykits.admin.modules.dashboard.mapper.DashboardMapper;
import cn.easykits.admin.modules.dashboard.vo.DashboardOverviewVO;
import cn.easykits.admin.modules.dashboard.vo.MemberGrowthVO;
import cn.easykits.admin.modules.dashboard.vo.TopGoodsVO;
import cn.easykits.admin.modules.dashboard.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分析页统计：全部为轻量聚合查询，直接走 Mapper，不引入额外缓存。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderMapper orderMapper;
    private final GoodsMapper goodsMapper;
    private final DashboardMapper dashboardMapper;

    /**
     * 订单趋势：按下单日期聚合（排除已取消），无单日期补零。
     */
    public List<TrendPointVO> orderTrend(int days) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        Map<String, TrendPointVO> byDate = new HashMap<>();
        for (TrendPointVO point : dashboardMapper.orderTrend(start)) {
            byDate.put(point.date(), point);
        }
        List<TrendPointVO> result = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            String date = start.plusDays(i).toLocalDate().toString();
            result.add(byDate.getOrDefault(date, new TrendPointVO(date, 0, BigDecimal.ZERO)));
        }
        return result;
    }

    /**
     * 商品销量排行：按明细快照聚合（排除已取消订单）。
     */
    public List<TopGoodsVO> topGoods(int days, int limit) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        return dashboardMapper.topGoods(start, Math.min(Math.max(limit, 1), 20));
    }

    /**
     * 会员增长：按注册日期聚合，无注册日期补零。
     */
    public List<MemberGrowthVO> memberGrowth(int days) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        Map<String, MemberGrowthVO> byDate = new HashMap<>();
        for (MemberGrowthVO point : dashboardMapper.memberGrowth(start)) {
            byDate.put(point.date(), point);
        }
        List<MemberGrowthVO> result = new ArrayList<>(days);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        for (int i = 0; i < days; i++) {
            String date = formatter.format(start.plusDays(i).toLocalDate());
            result.add(byDate.getOrDefault(date, new MemberGrowthVO(date, 0)));
        }
        return result;
    }

    public DashboardOverviewVO overview() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        // 今日订单：按创建时间统计（含待付款与已取消，反映真实下单量）
        long todayOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreatedAt, dayStart)
                .lt(Order::getCreatedAt, dayEnd));

        // 今日销售额：按支付时间统计，只算已支付订单
        BigDecimal todaySales = sumFirst(orderMapper.selectObjs(new QueryWrapper<Order>()
                .select("COALESCE(SUM(pay_amount), 0)")
                .ge("pay_time", dayStart)
                .lt("pay_time", dayEnd)));

        // 昨日同口径数据，用于计算环比
        LocalDateTime yesterdayStart = dayStart.minusDays(1);
        long yesterdayOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreatedAt, yesterdayStart)
                .lt(Order::getCreatedAt, dayStart));
        BigDecimal yesterdaySales = sumFirst(orderMapper.selectObjs(new QueryWrapper<Order>()
                .select("COALESCE(SUM(pay_amount), 0)")
                .ge("pay_time", yesterdayStart)
                .lt("pay_time", dayStart)));

        long pendingDelivery = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, Order.STATUS_UNDELIVERED));

        long finishedOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, Order.STATUS_FINISHED));

        // 库存预警：库存 <= 预警阈值的商品数
        long stockWarning = goodsMapper.selectCount(new LambdaQueryWrapper<Goods>()
                .apply("stock <= stock_warn"));

        long onSaleGoods = goodsMapper.selectCount(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, 1));

        return new DashboardOverviewVO(
                todayOrders, chainRate(todayOrders, yesterdayOrders),
                todaySales, chainRate(todaySales, yesterdaySales),
                pendingDelivery, finishedOrders, stockWarning, onSaleGoods);
    }

    /** 环比（%）：(今 - 昨) / 昨 * 100，保留 1 位小数；昨日为 0 时今日有量记 +100，两者皆无记 0 */
    private static BigDecimal chainRate(long today, long yesterday) {
        if (yesterday == 0) {
            return BigDecimal.valueOf(today > 0 ? 100.0 : 0.0);
        }
        return BigDecimal.valueOf((today - yesterday) * 100.0 / yesterday)
                .setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal chainRate(BigDecimal today, BigDecimal yesterday) {
        if (yesterday.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(today.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0);
        }
        return today.subtract(yesterday)
                .multiply(BigDecimal.valueOf(100))
                .divide(yesterday, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal sumFirst(List<Object> objs) {
        if (objs == null || objs.isEmpty() || objs.getFirst() == null) {
            return BigDecimal.ZERO;
        }
        Object value = objs.getFirst();
        return switch (value) {
            case BigDecimal decimal -> decimal;
            case Number number -> BigDecimal.valueOf(number.doubleValue());
            default -> new BigDecimal(value.toString());
        };
    }
}
