package com.easymall.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商城装修页面（只读）：小程序端只消费已发布快照。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dc_page")
public class DecoratePage extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 1 首页 2 分类页 3 微页面 */
    private Integer type;

    /** 0 草稿 1 已发布 */
    private Integer status;

    private Integer isHome;

    private String previewCode;

    /** 编辑中的草稿 */
    private String schemaJson;

    /** 已发布快照，小程序优先读这份 */
    private String publishedJson;

    private LocalDateTime publishedAt;

    private String remark;
}
