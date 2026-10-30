package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商品评价：管理端可审核、回复、隐藏。审核通过后才对小程序端其他用户展示。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("o_order_comment")
public class OrderComment extends BaseEntity {

    /** 待审核：用户刚提交，仅本人可见 */
    public static final int AUDIT_PENDING = 0;
    /** 已通过：小程序端其他用户可见 */
    public static final int AUDIT_PASSED = 1;
    /** 已拒绝：不展示，仅本人可见 */
    public static final int AUDIT_REJECTED = 2;

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

    /** 1 显示，0 隐藏（对已通过审核的评价生效） */
    private Integer isShow;

    /** 审核状态：0 待审核，1 已通过，2 已拒绝 */
    private Integer auditStatus;

    /** 审核备注（拒绝原因） */
    private String auditRemark;

    private LocalDateTime auditAt;

    private LocalDateTime commentedAt;
}
