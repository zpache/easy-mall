package com.easymall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easymall.api.entity.Goods;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {
}
