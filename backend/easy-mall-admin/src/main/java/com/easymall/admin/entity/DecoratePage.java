package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商城装修页面：草稿与发布各存一份 JSON 快照，预览码供真机扫码查看。
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

    /** 是否设为商城首页 */
    private Integer isHome;

    /** 真机预览码 */
    private String previewCode;

    /** 当前编辑内容（草稿） */
    private String schemaJson;

    /** 已发布快照 */
    private String publishedJson;

    private LocalDateTime publishedAt;

    private String remark;
}
