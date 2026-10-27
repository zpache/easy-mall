package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员积分流水：消费获得 / 积分抵扣 / 人工调整，每次变动留痕。
 */
@Data
@TableName("m_points_log")
public class PointsLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 积分变动量，正得负耗；change 为 MySQL 保留字，需反引号转义 */
    @TableField("`change`")
    private Integer change;

    /** 变动后余额 */
    private Integer balance;

    /** earn 消费获得 / consume 积分抵扣 / adjust 人工调整 */
    private String type;

    /** 关联订单号 */
    private String orderNo;

    private String remark;

    private LocalDateTime createdAt;

    /** 操作人 ID（人工调整时） */
    private Long createdBy;
}
