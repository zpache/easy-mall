package com.easymall.api.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.api.common.api.PageResult;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.modules.address.entity.Address;
import com.easymall.api.modules.address.mapper.AddressMapper;
import com.easymall.api.modules.cart.entity.CartItem;
import com.easymall.api.modules.cart.mapper.CartItemMapper;
import com.easymall.api.modules.goods.entity.Goods;
import com.easymall.api.modules.goods.mapper.GoodsMapper;
import com.easymall.api.modules.order.dto.OrderCreateRequest;
import com.easymall.api.modules.order.entity.MallOrder;
import com.easymall.api.modules.order.entity.OrderItem;
import com.easymall.api.modules.order.mapper.MallOrderMapper;
import com.easymall.api.modules.order.mapper.OrderItemMapper;
import com.easymall.api.modules.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务。
 * 当前为单体应用，下单流程在本地事务中完成；后续拆分支付、发货时可在此处接入消息或状态机。
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MallOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final GoodsMapper goodsMapper;
    private final AddressMapper addressMapper;

    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(Long userId, OrderCreateRequest request) {
        Address address = addressMapper.selectById(request.addressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }

        List<CartItem> cartItems = cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getChecked, 1));
        if (cartItems.isEmpty()) {
            throw new BusinessException(ResultCode.CART_EMPTY);
        }

        List<Long> goodsIds = cartItems.stream().map(CartItem::getGoodsId).distinct().toList();
        Map<Long, Goods> goodsMap = goodsMapper.selectByIds(goodsIds).stream()
                .collect(Collectors.toMap(Goods::getId, Function.identity()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>(cartItems.size());
        for (CartItem cartItem : cartItems) {
            Goods goods = goodsMap.get(cartItem.getGoodsId());
            if (goods == null || !Integer.valueOf(1).equals(goods.getStatus())) {
                throw new BusinessException(ResultCode.GOODS_OFF_SHELF, "商品已下架，请重新选择");
            }
            // 带条件扣减库存，避免并发超卖
            int affected = goodsMapper.update(null, Wrappers.<Goods>lambdaUpdate()
                    .eq(Goods::getId, goods.getId())
                    .ge(Goods::getStock, cartItem.getQuantity())
                    .setSql("stock = stock - " + cartItem.getQuantity())
                    .setSql("sales = sales + " + cartItem.getQuantity()));
            if (affected == 0) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH, goods.getName() + " 库存不足");
            }

            BigDecimal subtotal = goods.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setGoodsId(goods.getId());
            orderItem.setGoodsName(goods.getName());
            orderItem.setGoodsImage(goods.getMainImage());
            orderItem.setPrice(goods.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalAmount(subtotal);
            orderItems.add(orderItem);
        }

        MallOrder order = new MallOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setFreightAmount(BigDecimal.ZERO);
        order.setPayAmount(totalAmount);
        order.setStatus(MallOrder.STATUS_UNPAID);
        order.setReceiverName(address.getReceiver());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetail());
        order.setRemark(request.remark());
        orderMapper.insert(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 已下单的购物车条目直接物理删除
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .in(CartItem::getId, cartItems.stream().map(CartItem::getId).toList()));

        return toVO(order, orderItems);
    }

    public PageResult<OrderVO> pageQuery(Long userId, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<MallOrder> wrapper = Wrappers.<MallOrder>lambdaQuery()
                .eq(MallOrder::getUserId, userId)
                .eq(status != null, MallOrder::getStatus, status)
                .orderByDesc(MallOrder::getId);
        Page<MallOrder> page = orderMapper.selectPage(Page.of(pageNum, pageSize), wrapper);

        List<Long> orderIds = page.getRecords().stream().map(MallOrder::getId).toList();
        Map<Long, List<OrderItem>> itemMap = orderIds.isEmpty()
                ? Map.of()
                : orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .in(OrderItem::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<OrderVO> records = page.getRecords().stream()
                .map(order -> toVO(order, itemMap.getOrDefault(order.getId(), List.of())))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public OrderVO detail(Long userId, Long id) {
        MallOrder order = requireOwnOrder(userId, id);
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, id));
        return toVO(order, items);
    }

    /**
     * 模拟支付。真实项目在这里对接微信支付，并改为异步回调推进状态。
     */
    public void pay(Long userId, Long id) {
        MallOrder order = requireOwnOrder(userId, id);
        int affected = orderMapper.update(null, Wrappers.<MallOrder>lambdaUpdate()
                .eq(MallOrder::getId, order.getId())
                .eq(MallOrder::getStatus, MallOrder.STATUS_UNPAID)
                .set(MallOrder::getStatus, MallOrder.STATUS_UNDELIVERED)
                .set(MallOrder::getPayTime, LocalDateTime.now()));
        if (affected == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不支持支付");
        }
    }

    /**
     * 取消订单并回滚库存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long id) {
        MallOrder order = requireOwnOrder(userId, id);
        int affected = orderMapper.update(null, Wrappers.<MallOrder>lambdaUpdate()
                .eq(MallOrder::getId, order.getId())
                .eq(MallOrder::getStatus, MallOrder.STATUS_UNPAID)
                .set(MallOrder::getStatus, MallOrder.STATUS_CANCELLED)
                .set(MallOrder::getCancelTime, LocalDateTime.now()));
        if (affected == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不支持取消");
        }
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            goodsMapper.update(null, Wrappers.<Goods>lambdaUpdate()
                    .eq(Goods::getId, item.getGoodsId())
                    .setSql("stock = stock + " + item.getQuantity())
                    .setSql("sales = GREATEST(sales - " + item.getQuantity() + ", 0)"));
        }
    }

    private MallOrder requireOwnOrder(Long userId, Long id) {
        MallOrder order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private String generateOrderNo() {
        return LocalDateTime.now().format(ORDER_NO_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private OrderVO toVO(MallOrder order, List<OrderItem> items) {
        return new OrderVO(
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                OrderVO.statusText(order.getStatus()),
                order.getTotalAmount(),
                order.getFreightAmount(),
                order.getPayAmount(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getRemark(),
                order.getCreatedAt(),
                items);
    }
}
