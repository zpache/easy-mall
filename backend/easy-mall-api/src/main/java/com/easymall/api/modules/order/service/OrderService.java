package com.easymall.api.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.api.common.api.PageResult;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.entity.Address;
import com.easymall.api.entity.CartItem;
import com.easymall.api.entity.Coupon;
import com.easymall.api.entity.CouponUser;
import com.easymall.api.entity.Goods;
import com.easymall.api.entity.MallOrder;
import com.easymall.api.entity.OrderItem;
import com.easymall.api.mapper.AddressMapper;
import com.easymall.api.mapper.CartItemMapper;
import com.easymall.api.mapper.CouponMapper;
import com.easymall.api.mapper.CouponUserMapper;
import com.easymall.api.mapper.GoodsMapper;
import com.easymall.api.mapper.MallOrderMapper;
import com.easymall.api.mapper.OrderItemMapper;
import com.easymall.api.modules.freight.service.FreightService;
import com.easymall.api.modules.comment.service.CommentService;
import com.easymall.api.modules.order.dto.BuyNowRequest;
import com.easymall.api.modules.order.dto.OrderCreateRequest;
import com.easymall.api.modules.order.vo.OrderVO;
import com.easymall.api.modules.order.vo.UsableCouponVO;
import com.easymall.api.modules.member.service.MemberDiscountService;
import com.easymall.api.modules.member.service.MemberPointService;
import com.easymall.api.modules.setting.service.PaymentConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务。
 * 当前为单体应用，下单流程在本地事务中完成；后续拆分支付、发货时可在此处接入消息或状态机。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 会员券状态：与 mk_coupon_user 口径一致 */
    private static final int COUPON_UNUSED = 0;
    private static final int COUPON_USED = 1;

    private final MallOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final GoodsMapper goodsMapper;
    private final AddressMapper addressMapper;
    private final CouponMapper couponMapper;
    private final CouponUserMapper couponUserMapper;
    private final FreightService freightService;
    private final PaymentConfigService paymentConfigService;
    private final MemberPointService memberPointService;
    private final MemberDiscountService memberDiscountService;
    private final CommentService commentService;

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

            OrderItem orderItem = new OrderItem();
            orderItem.setGoodsId(goods.getId());
            orderItem.setGoodsName(goods.getName());
            orderItem.setGoodsImage(goods.getMainImage());
            orderItem.setPrice(goods.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalAmount(subtotal);
            orderItems.add(orderItem);
        }

        OrderVO order = insertOrder(userId, address, request.remark(), request.couponUserId(), orderItems);

        // 已下单的购物车条目直接物理删除
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .in(CartItem::getId, cartItems.stream().map(CartItem::getId).toList()));

        return order;
    }

    /**
     * 立即购买：不走购物车，按指定商品与数量直接生成订单，不影响购物车数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createBuyNow(Long userId, BuyNowRequest request) {
        Address address = addressMapper.selectById(request.addressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }

        Goods goods = goodsMapper.selectById(request.goodsId());
        if (goods == null || !Integer.valueOf(1).equals(goods.getStatus())) {
            throw new BusinessException(ResultCode.GOODS_OFF_SHELF, "商品已下架，请重新选择");
        }

        // 带条件扣减库存，避免并发超卖
        int affected = goodsMapper.update(null, Wrappers.<Goods>lambdaUpdate()
                .eq(Goods::getId, goods.getId())
                .ge(Goods::getStock, request.quantity())
                .setSql("stock = stock - " + request.quantity())
                .setSql("sales = sales + " + request.quantity()));
        if (affected == 0) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH, goods.getName() + " 库存不足");
        }

        BigDecimal subtotal = goods.getPrice().multiply(BigDecimal.valueOf(request.quantity()));
        OrderItem orderItem = new OrderItem();
        orderItem.setGoodsId(goods.getId());
        orderItem.setGoodsName(goods.getName());
        orderItem.setGoodsImage(goods.getMainImage());
        orderItem.setPrice(goods.getPrice());
        orderItem.setQuantity(request.quantity());
        orderItem.setTotalAmount(subtotal);

        return insertOrder(userId, address, request.remark(), request.couponUserId(), List.of(orderItem));
    }

    /**
     * 建单公共尾部：优惠券核销 + 订单与明细落库。购物车下单与立即购买共用，均在调用方事务内执行。
     */
    private OrderVO insertOrder(Long userId, Address address, String remark, Long couponUserId,
                                List<OrderItem> orderItems) {
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String orderNo = generateOrderNo();

        // 会员等级折扣：只优惠商品金额，折扣率来自当前会员等级（可在「商城设置 - 会员配置」关闭）
        MemberDiscountService.LevelDiscount levelDiscount =
                memberDiscountService.calculate(userId, totalAmount);
        BigDecimal memberDiscount = levelDiscount.discountAmount();
        BigDecimal discountedTotal = totalAmount.subtract(memberDiscount).max(BigDecimal.ZERO);

        // 优惠券：服务端按折后商品金额重新校验与计算，不信任前端传来的抵扣额
        Long couponId = null;
        BigDecimal couponAmount = BigDecimal.ZERO;
        if (couponUserId != null) {
            CouponRedemption redemption = redeemCoupon(userId, couponUserId, orderNo, discountedTotal);
            couponId = redemption.couponId();
            couponAmount = redemption.amount();
        }

        // 运费：按收货地址与折后商品金额服务端计算，不配送地区直接阻断下单（优惠不抵运费）
        BigDecimal freightAmount = freightService.calculateOrThrow(address, discountedTotal);

        MallOrder order = new MallOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setFreightAmount(freightAmount);
        order.setCouponId(couponId);
        order.setCouponAmount(couponAmount);
        order.setMemberDiscount(memberDiscount);
        order.setMemberLevelName(levelDiscount.effective() ? levelDiscount.levelName() : null);
        order.setPayAmount(discountedTotal.subtract(couponAmount).max(BigDecimal.ZERO).add(freightAmount));
        order.setRefundedAmount(BigDecimal.ZERO);
        order.setStatus(MallOrder.STATUS_UNPAID);
        order.setReceiverName(address.getReceiver());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetail());
        order.setRemark(remark);
        orderMapper.insert(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        return toVO(order, orderItems, Set.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public PageResult<OrderVO> pageQuery(Long userId, Integer status, long pageNum, long pageSize) {
        // 先兜底关闭该用户已超时的待付款订单，保证列表里的倒计时/状态是最新的
        closeExpiredOrders(userId);
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

        // 一次性查出本页订单的已评价商品，避免逐单查询
        Map<Long, Set<Long>> commentedMap = commentService.commentedGoodsByOrder(orderIds);
        List<OrderVO> records = page.getRecords().stream()
                .map(order -> toVO(order, itemMap.getOrDefault(order.getId(), List.of()),
                        commentedMap.getOrDefault(order.getId(), Set.of())))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderVO detail(Long userId, Long id) {
        // 先校验归属（不存在直接抛错，不产生任何写操作），再兜底关单
        requireOwnOrder(userId, id);
        closeExpiredOrders(userId);
        // 关单可能刚把订单状态改掉，重新读一次拿最新状态
        MallOrder order = requireOwnOrder(userId, id);
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, id));
        Set<Long> commented = commentService.commentedGoodsByOrder(List.of(id))
                .getOrDefault(id, Set.of());
        return toVO(order, items, commented);
    }

    /**
     * 模拟支付（渠道开关由「商城设置 - 支付配置」控制）。
     * 关闭模拟支付后本接口不可用，需走微信支付正式流程。
     *
     * <p>注意：本方法刻意不加事务——超时分支要先落库关单、再抛出「已超时」，
     * 若包在同一事务里，抛异常会把关单结果一起回滚。成功分支只是单条条件更新，本身就原子。</p>
     */
    public void pay(Long userId, Long id) {
        if (!paymentConfigService.mockPayEnabled()) {
            throw new BusinessException(ResultCode.PAY_CHANNEL_CLOSED, "模拟支付已关闭，请使用微信支付");
        }
        MallOrder order = requireOwnOrder(userId, id);
        // 超过支付时限的订单先关闭再拒绝支付，避免「倒计时已结束却还能付」
        LocalDateTime deadline = payDeadline(order);
        if (deadline != null && !LocalDateTime.now().isBefore(deadline)) {
            closeOrder(order, LocalDateTime.now());
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单已超过支付时限，已自动关闭");
        }
        int affected = orderMapper.update(null, Wrappers.<MallOrder>lambdaUpdate()
                .eq(MallOrder::getId, order.getId())
                .eq(MallOrder::getStatus, MallOrder.STATUS_UNPAID)
                .set(MallOrder::getStatus, MallOrder.STATUS_UNDELIVERED)
                .set(MallOrder::getPayTime, LocalDateTime.now()));
        if (affected == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不支持支付");
        }
        // 支付成功即结算会员权益（积分 / 累计消费 / 等级）；发放失败不影响支付结果，由补发任务兜底
        try {
            memberPointService.awardForOrder(order);
        } catch (Exception e) {
            log.error("订单积分发放失败，等待补发任务重试: orderNo={}", order.getOrderNo(), e);
        }
    }

    /**
     * 支付成功入账（微信支付回调调用）。
     *
     * <p>幂等：带 status=待付款 的条件更新，重复回调只会成功一次。
     * 金额校验由调用方先做，这里再校验一次总量避免误传。</p>
     *
     * @param orderNo       商户订单号
     * @param transactionId 微信支付订单号（仅用于日志对账）
     * @param paidFen       微信实付金额（分），<=0 表示不校验
     * @return true 表示处理完成（含重复回调）；false 表示金额不一致，需要人工介入
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markPaid(String orderNo, String transactionId, long paidFen) {
        MallOrder order = orderMapper.selectOne(Wrappers.<MallOrder>lambdaQuery()
                .eq(MallOrder::getOrderNo, orderNo));
        if (order == null) {
            log.error("支付回调找不到订单: orderNo={}, transactionId={}", orderNo, transactionId);
            return false;
        }
        long expectFen = order.getPayAmount() == null ? 0L
                : order.getPayAmount().multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).longValue();
        if (paidFen > 0 && paidFen != expectFen) {
            log.error("支付金额不一致，拒绝入账: orderNo={}, 应付={}分, 实付={}分", orderNo, expectFen, paidFen);
            return false;
        }

        int affected = orderMapper.update(null, Wrappers.<MallOrder>lambdaUpdate()
                .eq(MallOrder::getId, order.getId())
                .eq(MallOrder::getStatus, MallOrder.STATUS_UNPAID)
                .set(MallOrder::getStatus, MallOrder.STATUS_UNDELIVERED)
                .set(MallOrder::getPayTime, LocalDateTime.now()));
        if (affected == 0) {
            MallOrder latest = orderMapper.selectById(order.getId());
            if (latest != null && latest.getStatus() == MallOrder.STATUS_CANCELLED) {
                // 订单已被超时关闭但钱到账了：标记退款中，让后台能看到并原路退回
                orderMapper.update(null, Wrappers.<MallOrder>lambdaUpdate()
                        .eq(MallOrder::getId, order.getId())
                        .set(MallOrder::getRefundStatus, 1));
                log.error("订单已关闭却收到支付，已标记为退款中，需人工退款: orderNo={}, transactionId={}",
                        orderNo, transactionId);
            }
            // 重复回调或订单已是其它已支付状态，视为处理成功，避免微信持续重试
            return true;
        }

        order.setStatus(MallOrder.STATUS_UNDELIVERED);
        order.setPayTime(LocalDateTime.now());
        try {
            memberPointService.awardForOrder(order);
        } catch (Exception e) {
            log.error("支付回调后积分发放失败，等待补发任务重试: orderNo={}", orderNo, e);
        }
        log.info("微信支付入账成功: orderNo={}, transactionId={}, 金额={}分", orderNo, transactionId, paidFen);
        return true;
    }

    /**
     * 取消订单并回滚库存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long id) {
        MallOrder order = requireOwnOrder(userId, id);
        if (!closeOrder(order, LocalDateTime.now())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不支持取消");
        }
    }

    /**
     * 确认收货：待收货 → 已完成，记录完成时间。
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Long userId, Long id) {
        MallOrder order = requireOwnOrder(userId, id);
        if (!finishOrder(order, LocalDateTime.now())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不支持确认收货");
        }
    }

    /**
     * 待收货 → 已完成。带 status 条件的更新保证幂等：
     * 用户重复点击、多端同时操作、定时任务与手动操作并发，都只会成功一次。
     */
    private boolean finishOrder(MallOrder order, LocalDateTime finishTime) {
        int affected = orderMapper.update(null, Wrappers.<MallOrder>lambdaUpdate()
                .eq(MallOrder::getId, order.getId())
                .eq(MallOrder::getStatus, MallOrder.STATUS_UNRECEIVED)
                .set(MallOrder::getStatus, MallOrder.STATUS_FINISHED)
                .set(MallOrder::getFinishTime, finishTime));
        if (affected == 0) {
            return false;
        }
        order.setStatus(MallOrder.STATUS_FINISHED);
        order.setFinishTime(finishTime);
        return true;
    }

    /**
     * 发货超时自动确认收货：定时任务调用，天数为 0 或负数表示不自动完成。
     *
     * @return 本次自动完成的订单数
     */
    @Transactional(rollbackFor = Exception.class)
    public int autoFinishOrders() {
        int days = paymentConfigService.autoReceiveDays();
        if (days <= 0) {
            return 0;
        }
        LocalDateTime deadline = LocalDateTime.now().minusDays(days);
        List<MallOrder> list = orderMapper.selectList(Wrappers.<MallOrder>lambdaQuery()
                .eq(MallOrder::getStatus, MallOrder.STATUS_UNRECEIVED)
                .isNotNull(MallOrder::getDeliveryTime)
                .lt(MallOrder::getDeliveryTime, deadline)
                .orderByAsc(MallOrder::getId)
                .last("limit 200"));
        int finished = 0;
        for (MallOrder order : list) {
            if (finishOrder(order, LocalDateTime.now())) {
                finished++;
            }
        }
        return finished;
    }

    /**
     * 支付截止时间：待付款订单 = 下单时间 + 「商城设置 - 支付配置」里的支付超时分钟数。
     * 非待付款订单返回 null。
     */
    private LocalDateTime payDeadline(MallOrder order) {
        if (order.getStatus() == null || order.getStatus() != MallOrder.STATUS_UNPAID
                || order.getCreatedAt() == null) {
            return null;
        }
        return order.getCreatedAt().plusMinutes(paymentConfigService.orderPayTimeoutMinutes());
    }

    /**
     * 关闭订单（取消 / 超时关单共用）：置为已取消、回滚库存与销量、退回优惠券。
     * 条件更新成功才做回滚，重复调用（如定时任务与用户操作并发）不会重复回滚。
     */
    private boolean closeOrder(MallOrder order, LocalDateTime cancelTime) {
        int affected = orderMapper.update(null, Wrappers.<MallOrder>lambdaUpdate()
                .eq(MallOrder::getId, order.getId())
                .eq(MallOrder::getStatus, MallOrder.STATUS_UNPAID)
                .set(MallOrder::getStatus, MallOrder.STATUS_CANCELLED)
                .set(MallOrder::getCancelTime, cancelTime));
        if (affected == 0) {
            return false;
        }
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            goodsMapper.update(null, Wrappers.<Goods>lambdaUpdate()
                    .eq(Goods::getId, item.getGoodsId())
                    .setSql("stock = stock + " + item.getQuantity())
                    .setSql("sales = GREATEST(sales - " + item.getQuantity() + ", 0)"));
        }
        rollbackCoupon(order);
        // 同步内存对象，避免同一请求里继续用旧状态渲染
        order.setStatus(MallOrder.STATUS_CANCELLED);
        order.setCancelTime(cancelTime);
        return true;
    }

    /**
     * 全局超时关单：定时任务调用，按当前支付超时配置关闭所有已超时的待付款订单。
     *
     * @return 本次关闭的订单数
     */
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredOrders() {
        return closeExpiredOrders(null);
    }

    /**
     * 超时关单（可按用户过滤）：进订单列表/详情时先兜底关闭，保证用户立刻看到正确状态。
     */
    private int closeExpiredOrders(Long userId) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(paymentConfigService.orderPayTimeoutMinutes());
        List<MallOrder> expired = orderMapper.selectList(Wrappers.<MallOrder>lambdaQuery()
                .eq(userId != null, MallOrder::getUserId, userId)
                .eq(MallOrder::getStatus, MallOrder.STATUS_UNPAID)
                .lt(MallOrder::getCreatedAt, deadline)
                .orderByAsc(MallOrder::getId)
                .last("limit 200"));
        int closed = 0;
        for (MallOrder order : expired) {
            if (closeOrder(order, LocalDateTime.now())) {
                closed++;
            }
        }
        return closed;
    }

    /** 取消订单时回滚优惠券：置回未使用并清空核销信息，券模板核销数 -1（展示层会把已过有效期的券归一为已过期） */
    private void rollbackCoupon(MallOrder order) {
        // 按 couponId 判断是否用过券：折扣型券（或极小金额订单）实际抵扣可能算出来是 0.00，
        // 用金额判断会把这些券永久卡在「已使用」状态，用户既用不了也退不回
        if (order.getCouponId() == null || order.getOrderNo() == null) {
            return;
        }
        int affected = couponUserMapper.update(null, Wrappers.<CouponUser>lambdaUpdate()
                .eq(CouponUser::getOrderNo, order.getOrderNo())
                .eq(CouponUser::getStatus, COUPON_USED)
                .set(CouponUser::getStatus, COUPON_UNUSED)
                .set(CouponUser::getOrderNo, null)
                .set(CouponUser::getUsedAt, null));
        if (affected > 0) {
            couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                    .eq(Coupon::getId, order.getCouponId())
                    .setSql("used_count = GREATEST(used_count - 1, 0)"));
        }
    }

    /** 券核销结果：券模板 id + 实际抵扣金额 */
    private record CouponRedemption(Long couponId, BigDecimal amount) {
    }

    /**
     * 核销优惠券：校验归属、状态、有效期与门槛后，带条件更新为已使用并回填订单号。
     * 在下单事务内执行，任何校验失败都会抛异常回滚整个订单。
     */
    private CouponRedemption redeemCoupon(Long userId, Long couponUserId, String orderNo, BigDecimal totalAmount) {
        CouponUser record = couponUserMapper.selectById(couponUserId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券不存在");
        }
        if (record.getStatus() == null || record.getStatus() != COUPON_UNUSED) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券已被使用或已过期，请重新选择");
        }
        Coupon coupon = couponMapper.selectById(record.getCouponId());
        if (coupon == null || !Integer.valueOf(1).equals(coupon.getStatus())) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券已失效");
        }
        LocalDateTime now = LocalDateTime.now();
        if (isExpired(record, coupon, now)) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券已过有效期");
        }
        BigDecimal threshold = coupon.getThreshold() == null ? BigDecimal.ZERO : coupon.getThreshold();
        if (totalAmount.compareTo(threshold) < 0) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE,
                    "未满足" + coupon.getName() + "使用门槛（满" + threshold.stripTrailingZeros().toPlainString() + "元可用）");
        }

        // 带条件更新核销，防止同一张券在并发下单中被重复使用
        int affected = couponUserMapper.update(null, Wrappers.<CouponUser>lambdaUpdate()
                .eq(CouponUser::getId, record.getId())
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getStatus, COUPON_UNUSED)
                .set(CouponUser::getStatus, COUPON_USED)
                .set(CouponUser::getOrderNo, orderNo)
                .set(CouponUser::getUsedAt, now));
        if (affected == 0) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券已被使用，请重新选择");
        }
        couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                .eq(Coupon::getId, coupon.getId())
                .setSql("used_count = used_count + 1"));

        return new CouponRedemption(coupon.getId(), calcDiscount(coupon, totalAmount));
    }

    /** 券是否已过有效期：记录上的 expired_at 优先；固定日期券取 valid_end 当天末尾；N 天券取领取时间 + N 天 */
    private boolean isExpired(CouponUser record, Coupon coupon, LocalDateTime now) {
        if (record.getExpiredAt() != null) {
            return record.getExpiredAt().isBefore(now);
        }
        if (coupon.getValidType() != null && coupon.getValidType() == 2
                && coupon.getValidDays() != null && record.getReceivedAt() != null) {
            return record.getReceivedAt().plusDays(coupon.getValidDays()).isBefore(now);
        }
        return coupon.getValidEnd() != null && coupon.getValidEnd().atTime(23, 59, 59).isBefore(now);
    }

    /**
     * 计算券在本单的抵扣金额：满减/无门槛券抵面额，折扣券按 (100 - 折扣) / 100 计算优惠；
     * 抵扣不超过订单金额，最多抵到 0 元。
     */
    private BigDecimal calcDiscount(Coupon coupon, BigDecimal totalAmount) {
        BigDecimal saved;
        if (coupon.getType() != null && coupon.getType() == 2 && coupon.getDiscount() != null) {
            // discount 为百分比：90 表示 9 折，优惠 = 总价 × 10%
            saved = totalAmount.multiply(BigDecimal.valueOf(100).subtract(coupon.getDiscount()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            saved = coupon.getAmount() == null ? BigDecimal.ZERO : coupon.getAmount();
        }
        return saved.min(totalAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 结算页可用优惠券：按购物车已勾选商品金额实时计算每张券的抵扣与可用性；
     * 传入 goodsId + quantity 时改为按立即购买的单个商品金额计算。
     * 可用的排前面，同可用性下抵扣大的优先、到期近的其次。
     */
    public List<UsableCouponVO> usableCoupons(Long userId, Long goodsId, Integer quantity) {
        BigDecimal totalAmount = goodsId != null && quantity != null && quantity > 0
                ? singleGoodsTotal(goodsId, quantity)
                : checkedCartTotal(userId);
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        // 券门槛与抵扣按「会员折扣后的商品金额」计算，与下单口径保持一致
        BigDecimal discountedTotal = totalAmount
                .subtract(memberDiscountService.calculate(userId, totalAmount).discountAmount())
                .max(BigDecimal.ZERO);

        List<CouponUser> records = couponUserMapper.selectList(Wrappers.<CouponUser>lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getStatus, COUPON_UNUSED)
                .orderByDesc(CouponUser::getId));
        LocalDateTime now = LocalDateTime.now();
        List<UsableCouponVO> list = new ArrayList<>(records.size());
        for (CouponUser record : records) {
            Coupon coupon = couponMapper.selectById(record.getCouponId());
            if (coupon == null || !Integer.valueOf(1).equals(coupon.getStatus())) {
                continue;
            }
            boolean expired = isExpired(record, coupon, now);
            BigDecimal threshold = coupon.getThreshold() == null ? BigDecimal.ZERO : coupon.getThreshold();
            boolean thresholdMet = discountedTotal.compareTo(threshold) >= 0;
            boolean usable = !expired && thresholdMet;

            list.add(new UsableCouponVO(
                    record.getId(),
                    coupon.getName(),
                    valueText(coupon),
                    threshold.compareTo(BigDecimal.ZERO) <= 0 ? "无门槛" : "满" + threshold.stripTrailingZeros().toPlainString() + "元可用",
                    usable ? calcDiscount(coupon, discountedTotal) : BigDecimal.ZERO,
                    usable,
                    expired ? "已过有效期" : thresholdMet ? null : "还差"
                            + threshold.subtract(discountedTotal).stripTrailingZeros().toPlainString() + "元可用",
                    expireText(record, coupon)));
        }
        list.sort((a, b) -> {
            if (a.usable() != b.usable()) {
                return a.usable() ? -1 : 1;
            }
            int byDiscount = b.discountAmount().compareTo(a.discountAmount());
            if (byDiscount != 0) {
                return byDiscount;
            }
            return String.valueOf(a.expireAt()).compareTo(String.valueOf(b.expireAt()));
        });
        return list;
    }

    /** 购物车已勾选商品的合计金额（不含运费），与下单口径一致 */
    private BigDecimal checkedCartTotal(Long userId) {
        List<CartItem> cartItems = cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getChecked, 1));
        if (cartItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<Long> goodsIds = cartItems.stream().map(CartItem::getGoodsId).distinct().toList();
        Map<Long, Goods> goodsMap = goodsMapper.selectByIds(goodsIds).stream()
                .collect(Collectors.toMap(Goods::getId, Function.identity()));
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Goods goods = goodsMap.get(cartItem.getGoodsId());
            if (goods != null) {
                total = total.add(goods.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            }
        }
        return total;
    }

    /** 单个商品在指定数量下的合计金额（立即购买口径），商品缺失按 0 处理 */
    private BigDecimal singleGoodsTotal(Long goodsId, Integer quantity) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            return BigDecimal.ZERO;
        }
        return goods.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    private String valueText(Coupon coupon) {
        if (coupon.getType() != null && coupon.getType() == 2 && coupon.getDiscount() != null) {
            return coupon.getDiscount().divide(BigDecimal.TEN).stripTrailingZeros().toPlainString() + "折";
        }
        BigDecimal amount = coupon.getAmount() == null ? BigDecimal.ZERO : coupon.getAmount();
        return "¥" + amount.stripTrailingZeros().toPlainString();
    }

    /** 到期时间文案，null 表示长期有效 */
    private String expireText(CouponUser record, Coupon coupon) {
        LocalDate date = null;
        if (record.getExpiredAt() != null) {
            date = record.getExpiredAt().toLocalDate();
        } else if (coupon.getValidType() != null && coupon.getValidType() == 2
                && coupon.getValidDays() != null && record.getReceivedAt() != null) {
            date = record.getReceivedAt().toLocalDate().plusDays(coupon.getValidDays());
        } else if (coupon.getValidEnd() != null) {
            date = coupon.getValidEnd();
        }
        return date == null ? null : date.toString().replace('-', '.');
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

    private OrderVO toVO(MallOrder order, List<OrderItem> items, Set<Long> commentedGoods) {
        LocalDateTime payDeadline = payDeadline(order);
        Long payRemainSeconds = payDeadline == null
                ? null
                : Math.max(Duration.between(LocalDateTime.now(), payDeadline).getSeconds(), 0L);
        // 标记每个商品是否已评价；已完成订单再统计待评价数量，供列表页显示评价入口
        int pendingCommentCount = 0;
        for (OrderItem item : items) {
            boolean commented = commentedGoods != null && commentedGoods.contains(item.getGoodsId());
            item.setCommented(commented);
            if (!commented && Integer.valueOf(MallOrder.STATUS_FINISHED).equals(order.getStatus())) {
                pendingCommentCount++;
            }
        }
        return new OrderVO(
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                OrderVO.statusText(order.getStatus()),
                order.getRefundStatus() == null ? 0 : order.getRefundStatus(),
                OrderVO.refundStatusText(order.getRefundStatus()),
                order.getTotalAmount(),
                order.getFreightAmount(),
                order.getCouponAmount(),
                order.getMemberDiscount() == null ? BigDecimal.ZERO : order.getMemberDiscount(),
                order.getMemberLevelName(),
                order.getPayAmount(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getDeliveryCompany(),
                order.getDeliveryNo(),
                order.getDeliveryTime(),
                order.getRemark(),
                order.getCreatedAt(),
                payDeadline,
                payRemainSeconds,
                items,
                pendingCommentCount);
    }
}
