package com.easymall.api.modules.cart.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.modules.cart.dto.CartAddRequest;
import com.easymall.api.modules.cart.entity.CartItem;
import com.easymall.api.modules.cart.mapper.CartItemMapper;
import com.easymall.api.modules.cart.vo.CartItemVO;
import com.easymall.api.modules.goods.entity.Goods;
import com.easymall.api.modules.goods.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车服务。
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final GoodsMapper goodsMapper;

    public List<CartItemVO> list() {
        Long userId = UserContext.userId();
        List<CartItem> items = cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getId));
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> goodsIds = items.stream().map(CartItem::getGoodsId).distinct().toList();
        Map<Long, Goods> goodsMap = goodsMapper.selectByIds(goodsIds).stream()
                .collect(Collectors.toMap(Goods::getId, Function.identity()));

        return items.stream().map(item -> {
            Goods goods = goodsMap.get(item.getGoodsId());
            if (goods == null) {
                return new CartItemVO(item.getId(), item.getGoodsId(), "商品已删除", null,
                        BigDecimal.ZERO, 0, item.getQuantity(), 0, BigDecimal.ZERO, true);
            }
            boolean invalid = !Integer.valueOf(1).equals(goods.getStatus())
                    || goods.getStock() < item.getQuantity();
            BigDecimal subtotal = goods.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartItemVO(item.getId(), item.getGoodsId(), goods.getName(), goods.getMainImage(),
                    goods.getPrice(), goods.getStock(), item.getQuantity(), item.getChecked(), subtotal, invalid);
        }).toList();
    }

    public long count() {
        Long userId = UserContext.userId();
        return cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                        .eq(CartItem::getUserId, userId))
                .stream()
                .mapToLong(CartItem::getQuantity)
                .sum();
    }

    public void add(CartAddRequest request) {
        Long userId = UserContext.userId();
        Goods goods = goodsMapper.selectById(request.goodsId());
        if (goods == null || !Integer.valueOf(1).equals(goods.getStatus())) {
            throw new BusinessException(ResultCode.GOODS_NOT_FOUND);
        }

        CartItem existing = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getGoodsId, request.goodsId()));

        int quantity = request.quantity() + (existing == null ? 0 : existing.getQuantity());
        if (goods.getStock() < quantity) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }

        if (existing == null) {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setGoodsId(request.goodsId());
            item.setQuantity(quantity);
            item.setChecked(1);
            cartItemMapper.insert(item);
        } else {
            existing.setQuantity(quantity);
            cartItemMapper.updateById(existing);
        }
    }

    public void updateQuantity(Long id, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BusinessException("数量至少为 1");
        }
        CartItem item = requireOwnItem(id);
        Goods goods = goodsMapper.selectById(item.getGoodsId());
        if (goods == null || goods.getStock() < quantity) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
    }

    /**
     * 勾选/取消勾选。id 为空表示全选或全不选。
     */
    public void updateChecked(Long id, Integer checked) {
        Long userId = UserContext.userId();
        int value = Integer.valueOf(1).equals(checked) ? 1 : 0;
        if (id == null) {
            cartItemMapper.update(null, Wrappers.<CartItem>lambdaUpdate()
                    .eq(CartItem::getUserId, userId)
                    .set(CartItem::getChecked, value));
            return;
        }
        CartItem item = requireOwnItem(id);
        item.setChecked(value);
        cartItemMapper.updateById(item);
    }

    public void remove(Long id) {
        CartItem item = requireOwnItem(id);
        cartItemMapper.deleteById(item.getId());
    }

    public void clear() {
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, UserContext.userId()));
    }

    private CartItem requireOwnItem(Long id) {
        CartItem item = cartItemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(UserContext.userId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "购物车条目不存在");
        }
        return item;
    }
}
