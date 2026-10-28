package com.easymall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easymall.api.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 原子占用一张库存：发放在 SQL 里判断余量，避免并发领券超发。
     * 返回 0 表示库存已耗尽（或被并发领完）。
     */
    @Update("""
            UPDATE mk_coupon
               SET send_count = send_count + 1
             WHERE id = #{id}
               AND deleted = 0
               AND status = 1
               AND (total_count = 0 OR total_count > send_count)
            """)
    int increaseSendCount(@Param("id") Long id);

    /** 领券记录减少时归还库存（事务回滚兜底用不到，保留给后台回收场景） */
    @Update("""
            UPDATE mk_coupon
               SET send_count = CASE WHEN send_count > 0 THEN send_count - 1 ELSE 0 END
             WHERE id = #{id}
            """)
    int decreaseSendCount(@Param("id") Long id);
}
