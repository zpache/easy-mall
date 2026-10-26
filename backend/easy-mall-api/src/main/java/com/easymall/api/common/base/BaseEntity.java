package com.easymall.api.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类：与 admin 模块共用同一套表结构约定。
 * 所有继承本类的表都带：
 * created_at / updated_at / created_by / updated_by / version / deleted。
 */
@Data
public abstract class BaseEntity implements Serializable {

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 创建人 ID；小程序首次微信登录自动注册时还没有登录态，为 null */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;

    /** 最后修改人 ID，每次 update 由 MetaObjectHandler 覆盖写入 */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    /**
     * 乐观锁版本号。
     * updateById 时 MyBatis-Plus 会把它拼进 where 条件（version = ?）并自动 +1，
     * 受影响行数为 0 说明期间已被别人改过。
     */
    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    private Integer version;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    @TableField(value = "deleted", select = false)
    private Integer deleted;
}
