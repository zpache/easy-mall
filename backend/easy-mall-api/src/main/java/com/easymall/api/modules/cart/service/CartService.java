package com.easymall.api.modules.cart.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.entity.CartItem;
import com.easymall.api.entity.Goods;
import com.easymall.api.entity.GoodsSku;
import com.easymall.api.mapper.CartItemMapper;
import com.easymall.api.mapper.GoodsMapper;
import com.easymall.api.mapper.GoodsSkuMapper;
import com.easymall.api.modules.cart.dto.CartAddRequest;
import com.easymall.api.modules.cart.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车服务。
 *
 * <p>多规格商品按「商品 + SKU」维度成行：同一商品的不同规格是不同条目，
 * 价格与库存以 SKU 为准；单规格商品（无 SKU）沿用商品表的价格与库存。</p>
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final GoodsMapper goodsMapper;
    private final GoodsSkuMapper goodsSkuMapper;

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
        Map<Long, GoodsSku> skuMap = loadSkus(items);

        return items.stream().map(item -> {
            Goods goods = goodsMap.get(item.getGoodsId());
            if (goods == null) {
                return new CartItemVO(item.getId(), item.getGoodsId(), "商品已删除", null,
                        BigDecimal.ZERO, 0, item.getQuantity(), 0, BigDecimal.ZERO, true,
                        item.getSkuId(), item.getSpecText());
            }
            GoodsSku sku = item.getSkuId() == null ? null : skuMap.get(item.getSkuId());
            // SKU 被删除或停用：该条目不可用，提示用户重新选择规格
            boolean skuInvalid = item.getSkuId() != null
                    && (sku == null || !Integer.valueOf(1).equals(sku.getStatus()));
            BigDecimal price = sku == null ? goods.getPrice() : sku.getPrice();
            int stock = sku == null ? nullSafe(goods.getStock()) : nullSafe(sku.getStock());
            boolean invalid = skuInvalid || !Integer.valueOf(1).equals(goods.getStatus())
                    || stock < item.getQuantity();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartItemVO(item.getId(), item.getGoodsId(), goods.getName(), goods.getMainImage(),
                    price, stock, item.getQuantity(), item.getChecked(), subtotal, invalid,
                    item.getSkuId(), item.getSpecText());
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
        GoodsSku sku = resolveSku(goods, request.skuId());
        int stock = sku == null ? nullSafe(goods.getStock()) : nullSafe(sku.getStock());

        CartItem existing = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getGoodsId, request.goodsId())
                .eq(request.skuId() != null, CartItem::getSkuId, request.skuId())
                .isNull(request.skuId() == null, CartItem::getSkuId));

        int quantity = request.quantity() + (existing == null ? 0 : existing.getQuantity());
        if (stock < quantity) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }

        if (existing == null) {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setGoodsId(request.goodsId());
            item.setSkuId(sku == null ? null : sku.getId());
            item.setSpecText(sku == null ? null : sku.getSpecText());
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
        if (goods == null) {
            throw new BusinessException(ResultCode.GOODS_NOT_FOUND);
        }
        int stock;
        if (item.getSkuId() != null) {
            GoodsSku sku = goodsSkuMapper.selectById(item.getSkuId());
            if (sku == null || !Integer.valueOf(1).equals(sku.getStatus())) {
                throw new BusinessException("商品规格已下架，请重新选择");
            }
            stock = nullSafe(sku.getStock());
        } else {
            stock = nullSafe(goods.getStock());
        }
        if (stock < quantity) {
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

    /**
     * 校验并返回 SKU：商品有规格时 skuId 必填且必须属于该商品且启用；
     * 单规格商品不传 skuId，忽略误传的值（防止把别的商品的 SKU 挂过来）。
     */
    private GoodsSku resolveSku(Goods goods, Long skuId) {
        long skuCount = goodsSkuMapper.selectCount(Wrappers.<GoodsSku>lambdaQuery()
                .eq(GoodsSku::getGoodsId, goods.getId())
                .eq(GoodsSku::getStatus, 1));
        if (skuCount == 0) {
            return null;
        }
        if (skuId == null) {
            throw new BusinessException("请选择商品规格");
        }
        GoodsSku sku = goodsSkuMapper.selectById(skuId);
        if (sku == null || !sku.getGoodsId().equals(goods.getId())
                || !Integer.valueOf(1).equals(sku.getStatus())) {
            throw new BusinessException("商品规格不存在或已下架，请重新选择");
        }
        return sku;
    }

    private Map<Long, GoodsSku> loadSkus(List<CartItem> items) {
        List<Long> skuIds = items.stream().map(CartItem::getSkuId)
                .filter(Objects::nonNull).distinct().toList();
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        return goodsSkuMapper.selectByIds(skuIds).stream()
                .collect(Collectors.toMap(GoodsSku::getId, Function.identity()));
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private CartItem requireOwnItem(Long id) {
        CartItem item = cartItemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(UserContext.userId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "购物车条目不存在");
        }
        return item;
    }
}
