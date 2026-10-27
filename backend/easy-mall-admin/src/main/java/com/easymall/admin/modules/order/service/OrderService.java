package com.easymall.admin.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.AppUser;
import com.easymall.admin.entity.Order;
import com.easymall.admin.entity.OrderItem;
import com.easymall.admin.mapper.AppUserMapper;
import com.easymall.admin.mapper.OrderItemMapper;
import com.easymall.admin.mapper.OrderMapper;
import com.easymall.admin.modules.order.dto.DeliverForm;
import com.easymall.admin.modules.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
     * 关闭订单：仅未发货（待付款 / 待发货）的订单可关闭，且不能存在进行中的售后。
     */
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id, String reason) {
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (!Objects.equals(order.getStatus(), Order.STATUS_UNPAID)
                && !Objects.equals(order.getStatus(), Order.STATUS_UNDELIVERED)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "订单已发货或已完成，不能关闭");
        }
        if (Objects.equals(order.getRefundStatus(), Order.REFUND_PROCESSING)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "该订单有进行中的退款申请，请先在售后退款中处理");
        }
        order.setStatus(Order.STATUS_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(StringUtils.hasText(reason) ? reason : "管理员关闭订单");
        if (!updateById(order)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "订单已被其他人处理，请刷新后重试");
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
