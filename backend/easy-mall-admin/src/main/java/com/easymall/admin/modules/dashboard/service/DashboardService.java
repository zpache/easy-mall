package com.easymall.admin.modules.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.easymall.admin.entity.Goods;
import com.easymall.admin.entity.Order;
import com.easymall.admin.mapper.GoodsMapper;
import com.easymall.admin.mapper.OrderMapper;
import com.easymall.admin.modules.dashboard.vo.DashboardOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分析页统计：全部为轻量聚合查询，直接走 Mapper，不引入额外缓存。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderMapper orderMapper;
    private final GoodsMapper goodsMapper;

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

        long totalOrders = orderMapper.selectCount(null);

        // 累计销售额：已付款（待发货/待收货/已完成）订单的应付金额合计
        BigDecimal totalSales = sumFirst(orderMapper.selectObjs(new QueryWrapper<Order>()
                .select("COALESCE(SUM(pay_amount), 0)")
                .in("status",
                        Order.STATUS_UNDELIVERED,
                        Order.STATUS_UNRECEIVED,
                        Order.STATUS_FINISHED)));

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
                todayOrders, totalOrders, todaySales, totalSales,
                pendingDelivery, finishedOrders, stockWarning, onSaleGoods);
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
