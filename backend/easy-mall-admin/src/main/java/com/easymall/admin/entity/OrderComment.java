package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商品评价：管理端可回复、隐藏，隐藏后小程序端不再展示。
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
