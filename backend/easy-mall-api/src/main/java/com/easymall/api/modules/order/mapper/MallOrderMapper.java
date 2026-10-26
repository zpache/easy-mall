package com.easymall.api.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easymall.api.modules.order.entity.MallOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MallOrderMapper extends BaseMapper<MallOrder> {
}
