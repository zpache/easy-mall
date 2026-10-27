package com.easymall.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easymall.admin.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 原子调整库存：把判断和更新放在同一条 SQL 里，避免并发下的超卖 / 覆盖。
     * stock + delta &lt; 0 时不会命中任何行，调用方据此提示库存不足。
     *
     * @param id    商品 ID
     * @param delta 变动量，正数入库、负数出库
     * @return 受影响行数，0 表示商品不存在或库存不足
     */
    @Update("""
            UPDATE mall_goods
            SET stock      = stock + #{delta},
                updated_at = NOW()
            WHERE id = #{id}
              AND deleted = 0
              AND stock + #{delta} >= 0
            """)
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);
}
