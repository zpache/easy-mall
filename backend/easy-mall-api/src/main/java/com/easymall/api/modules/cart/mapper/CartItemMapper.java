package com.easymall.api.modules.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easymall.api.modules.cart.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
