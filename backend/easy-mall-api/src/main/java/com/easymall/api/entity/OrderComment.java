package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商品评价：一个订单里的每个商品可评价一次，管理端可回复、隐藏。
 *
 * <p>同订单同商品的唯一性由数据库唯一索引 uk_comment_order_goods 保证，
 * 应用层另做一次前置校验给出友好提示。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("o_order_comment")
public class OrderComment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long goodsId;

    private String goodsName;

    private String goodsImage;

    private Long userId;

    private String userNickname;

    /** 评分 1-5 */
    private Integer score;

    private String content;

    /** 晒图，多张用英文逗号分隔 */
    private String images;

    /** 商家回复 */
    private String reply;

    private LocalDateTime repliedAt;

    /** 1 显示，0 隐藏 */
    private Integer isShow;

    private LocalDateTime commentedAt;
}
