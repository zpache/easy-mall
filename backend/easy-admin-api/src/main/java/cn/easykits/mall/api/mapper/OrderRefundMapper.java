package cn.easykits.mall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.easykits.mall.api.entity.OrderRefund;
import org.apache.ibatis.annotations.Mapper;

/**
 * 售后退款单。
 */
@Mapper
public interface OrderRefundMapper extends BaseMapper<OrderRefund> {
}
