package com.easymall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easymall.api.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
