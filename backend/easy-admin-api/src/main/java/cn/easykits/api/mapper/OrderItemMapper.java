package cn.easykits.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.easykits.api.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
