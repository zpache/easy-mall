package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品浏览足迹：每人每商品一行，重复浏览只刷新 browsed_at。
 */
@Data
@TableName("user_browse_log")
public class UserBrowseLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long goodsId;

    /** 最近浏览时间，足迹按它倒序展示 */
    @TableField(value = "browsed_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime browsedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
