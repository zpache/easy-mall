package com.easymall.admin.modules.dashboard.mapper;

import com.easymall.admin.modules.dashboard.vo.MemberGrowthVO;
import com.easymall.admin.modules.dashboard.vo.TopGoodsVO;
import com.easymall.admin.modules.dashboard.vo.TrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分析页聚合查询：纯只读统计，独立 Mapper 避免污染业务 Mapper。
 */
@Mapper
public interface DashboardMapper {

    /** 订单趋势：按下单日期聚合（排除已取消），空日期由服务端补零 */
    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS orders,
                   IFNULL(SUM(pay_amount), 0) AS salesAmount
            FROM o_order
            WHERE status <> 4 AND created_at >= #{start}
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            """)
    List<TrendPointVO> orderTrend(@Param("start") LocalDateTime start);

    /** 商品销量排行：按明细快照聚合（排除已取消订单） */
    @Select("""
            SELECT i.goods_name AS goodsName, SUM(i.quantity) AS quantity,
                   IFNULL(SUM(i.total_amount), 0) AS salesAmount
            FROM o_order_item i
            JOIN o_order o ON o.id = i.order_id
            WHERE o.status <> 4 AND o.created_at >= #{start}
            GROUP BY i.goods_name
            ORDER BY quantity DESC
            LIMIT #{limit}
            """)
    List<TopGoodsVO> topGoods(@Param("start") LocalDateTime start, @Param("limit") int limit);

    /** 会员增长：按注册日期聚合 */
    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS newMembers
            FROM m_member
            WHERE created_at >= #{start}
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            """)
    List<MemberGrowthVO> memberGrowth(@Param("start") LocalDateTime start);
}
