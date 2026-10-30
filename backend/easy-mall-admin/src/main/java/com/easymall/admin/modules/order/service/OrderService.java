package com.easymall.admin.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.AppUser;
import com.easymall.admin.entity.Coupon;
import com.easymall.admin.entity.CouponUser;
import com.easymall.admin.entity.Goods;
import com.easymall.admin.entity.Order;
import com.easymall.admin.entity.OrderItem;
import com.easymall.admin.mapper.AppUserMapper;
import com.easymall.admin.mapper.CouponMapper;
import com.easymall.admin.mapper.CouponUserMapper;
import com.easymall.admin.mapper.GoodsMapper;
import com.easymall.admin.mapper.OrderItemMapper;
import com.easymall.admin.mapper.OrderMapper;
import com.easymall.admin.modules.order.dto.DeliverForm;
import com.easymall.admin.modules.order.vo.OrderVO;
import com.easymall.admin.modules.pay.service.WxPayCloseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单服务：列表 / 详情、发货、关闭订单。
 */
@Service
@RequiredArgsConstructor
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    private final OrderItemMapper orderItemMapper;
    private final AppUserMapper appUserMapper;
    private final GoodsMapper goodsMapper;
    private final CouponMapper couponMapper;
    private final CouponUserMapper couponUserMapper;
    private final WxPayCloseService wxPayCloseService;

    /**
     * 订单分页：支持订单号 / 收货人 / 手机号模糊搜索，状态、售后状态与下单日期区间过滤。
     */
    public PageResult<OrderVO> pageQuery(long pageNum, long pageSize, String keyword, Integer status,
                                         Integer refundStatus, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Order::getOrderNo, keyword)
                        .or().like(Order::getReceiverName, keyword)
                        .or().like(Order::getReceiverPhone, keyword))
                .eq(status != null, Order::getStatus, status)
                .eq(refundStatus != null, Order::getRefundStatus, refundStatus)
                .ge(startDate != null, Order::getCreatedAt,
                        startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, Order::getCreatedAt,
                        endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByDesc(Order::getId);
        Page<Order> page = page(Page.of(pageNum, pageSize), wrapper);
        return new PageResult<>(toVOList(page.getRecords()), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    public OrderVO detail(Long id) {
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return toVOList(List.of(order)).getFirst();
    }

    /**
     * 按主键取订单实体，供物流查询等旁路功能使用。
     */
    public Order requireEntity(Long id) {
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return order;
    }

    /**
     * 发货：仅「待发货」订单可操作，发货后进入「待收货」。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deliver(Long id, DeliverForm form) {
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (!Objects.equals(order.getStatus(), Order.STATUS_UNDELIVERED)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "只有待发货的订单才能发货");
        }
        order.setStatus(Order.STATUS_UNRECEIVED);
        order.setDeliveryCompany(form.deliveryCompany());
        order.setDeliveryNo(form.deliveryNo());
        order.setDeliveryTime(LocalDateTime.now());
        if (!updateById(order)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "订单已被其他人处理，请刷新后重试");
        }
    }

    /**
     * 关闭订单：仅「待付款」且未收到支付的订单可关闭。
     *
     * <p>关闭时回滚库存与销量、退回优惠券，与小程序端取消/超时关单口径保持一致，
     * 避免后台关单造成库存永久少扣。</p>
     *
     * <p>已支付订单一律不允许直接关闭：钱已经收了，必须走售后退款（退款确认时回滚会员权益），
     * 否则会出现「订单已关闭但款项未退」的黑洞。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id, String reason) {
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (!Objects.equals(order.getStatus(), Order.STATUS_UNPAID)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "订单已支付或已发货，不能直接关闭");
        }
        if (order.getPayTime() != null) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "订单已支付，请通过「售后退款」处理，不能直接关闭");
        }
        if (Objects.equals(order.getRefundStatus(), Order.REFUND_PROCESSING)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "该订单有进行中的退款申请，请先在售后退款中处理");
        }
        // pay_time is null 必须进条件：上面的检查与这里的更新之间可能恰好收到支付回调，
        // 只带 status 条件会把刚支付成功的订单关掉（钱收了、库存券都退了且没有退款标记）
        int affected = baseMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, id)
                .isNull(Order::getPayTime)
                .eq(Order::getStatus, Order.STATUS_UNPAID)
                .set(Order::getStatus, Order.STATUS_CANCELLED)
                .set(Order::getCancelTime, LocalDateTime.now())
                .set(Order::getCancelReason, StringUtils.hasText(reason) ? reason : "管理员关闭订单"));
        if (affected == 0) {
            Order latest = getById(id);
            if (latest != null && latest.getPayTime() != null) {
                throw new BusinessException(ResultCode.DATA_CONFLICT, "订单刚刚完成支付，请通过「售后退款」处理");
            }
            throw new BusinessException(ResultCode.DATA_CONFLICT, "订单已被其他人处理，请刷新后重试");
        }
        rollbackStockAndCoupon(order);
        // 关闭微信侧预支付单：防止用户停留在收银台时又付款成功（best-effort，失败只记日志）
        if (order.getOrderNo() != null) {
            wxPayCloseService.close(order.getOrderNo());
        }
    }

    /**
     * 全额退款且订单仍在「待发货」时自动关闭订单：回滚库存、销量与优惠券。
     * 供售后退款确认后调用（与确认打款同一事务）。
     *
     * <p>部分退款不动订单状态；已发货订单的退货入库由运营在商品库存中手工调整，
     * 这里不自动回滚，避免「货还在路上、库存已经加回」。</p>
     *
     * @return true 表示本次关闭成功
     */
    public boolean closeRefundedOrder(Order order) {
        if (order == null || order.getPayAmount() == null) {
            return false;
        }
        BigDecimal refunded = order.getRefundedAmount() == null
                ? BigDecimal.ZERO : order.getRefundedAmount();
        if (refunded.compareTo(order.getPayAmount()) < 0) {
            return false;
        }
        int affected = baseMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, Order.STATUS_UNDELIVERED)
                .eq(Order::getRefundStatus, Order.REFUND_DONE)
                .set(Order::getStatus, Order.STATUS_CANCELLED)
                .set(Order::getCancelTime, LocalDateTime.now())
                .set(Order::getCancelReason, "全额退款完成，订单自动关闭"));
        if (affected == 0) {
            return false;
        }
        rollbackStockAndCoupon(order);
        return true;
    }

    /**
     * 关单回滚：库存加回、销量减回（不低于 0）、优惠券置回未使用并回退核销数。
     * 由带条件的状态更新保证只执行一次，不会重复回滚。
     */
    private void rollbackStockAndCoupon(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            goodsMapper.update(null, Wrappers.<Goods>lambdaUpdate()
                    .eq(Goods::getId, item.getGoodsId())
                    .setSql("stock = stock + " + item.getQuantity())
                    .setSql("sales = GREATEST(sales - " + item.getQuantity() + ", 0)"));
        }
        // 按 couponId 判断是否用过券：折扣型券（或极小金额订单）实际抵扣可能算出来是 0.00，
        // 用金额判断会把这些券永久卡在「已使用」状态，用户既用不了也退不回
        if (order.getCouponId() == null || order.getOrderNo() == null) {
            return;
        }
        List<CouponUser> records = couponUserMapper.selectList(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getOrderNo, order.getOrderNo())
                .eq(CouponUser::getStatus, 1));
        for (CouponUser record : records) {
            int affected = couponUserMapper.update(null, Wrappers.<CouponUser>lambdaUpdate()
                    .eq(CouponUser::getId, record.getId())
                    .eq(CouponUser::getStatus, 1)
                    .set(CouponUser::getStatus, 0)
                    .set(CouponUser::getOrderNo, null)
                    .set(CouponUser::getUsedAt, null));
            if (affected > 0) {
                couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                        .eq(Coupon::getId, record.getCouponId())
                        .setSql("used_count = GREATEST(used_count - 1, 0)"));
            }
        }
    }

    /**
     * 批量转 VO：明细与买家信息各查一次，避免逐行查库。
     */
    private List<OrderVO> toVOList(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemMap = orderItemMapper.selectList(
                        Wrappers.<OrderItem>lambdaQuery()
                                .in(OrderItem::getOrderId, orderIds)
                                .orderByAsc(OrderItem::getId))
                .stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        Set<Long> userIds = orders.stream().map(Order::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AppUser> userMap = userIds.isEmpty()
                ? Map.of()
                : appUserMapper.selectList(Wrappers.<AppUser>lambdaQuery().in(AppUser::getId, userIds)).stream()
                .collect(Collectors.toMap(AppUser::getId, user -> user, (a, b) -> a));

        return orders.stream().map(order -> {
            AppUser user = order.getUserId() == null ? null : userMap.get(order.getUserId());
            return OrderVO.of(order,
                    user == null ? null : user.getNickname(),
                    user == null ? null : user.getAvatar(),
                    itemMap.getOrDefault(order.getId(), List.of()));
        }).toList();
    }
}
