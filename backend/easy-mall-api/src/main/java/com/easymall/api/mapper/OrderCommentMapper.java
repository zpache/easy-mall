package com.easymall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easymall.api.entity.OrderComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface OrderCommentMapper extends BaseMapper<OrderComment> {

    /**
     * 商品评分概览：只统计审核通过且展示中的评价。
     * 直接写 SQL 是为了拿到确定的列别名（cnt / avgScore），避免 selectMaps 的 key 大小写不确定。
     */
    @Select("SELECT COUNT(*) AS cnt, IFNULL(AVG(score), 0) AS avgScore "
            + "FROM o_order_comment WHERE goods_id = #{goodsId} "
            + "AND audit_status = 1 AND is_show = 1 AND deleted = 0")
    Map<String, Object> statByGoods(@Param("goodsId") Long goodsId);
}
