package cn.easykits.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.easykits.api.entity.OrderRefund;
import org.apache.ibatis.annotations.Mapper;

/**
 * 售后退款单。
 */
@Mapper
public interface OrderRefundMapper extends BaseMapper<OrderRefund> {
}
