package cn.easykits.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.easykits.api.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
