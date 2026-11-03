package com.easymall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easymall.api.entity.UserBrowseLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserBrowseLogMapper extends BaseMapper<UserBrowseLog> {

    /** 足迹 upsert：同一商品重复浏览只刷新 browsed_at */
    @Insert("""
            INSERT INTO user_browse_log (user_id, goods_id, browsed_at, created_at)
            VALUES (#{userId}, #{goodsId}, NOW(), NOW())
            ON DUPLICATE KEY UPDATE browsed_at = NOW()
            """)
    int upsert(@Param("userId") Long userId, @Param("goodsId") Long goodsId);
}
