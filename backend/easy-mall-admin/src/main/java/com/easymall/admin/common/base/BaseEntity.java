package com.easymall.admin.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类：统一维护审计字段、乐观锁版本号与逻辑删除标记。
 * 所有继承本类的表都带：
 * created_at / updated_at / created_by / updated_by / version / deleted。
 */
@Data
public abstract class BaseEntity implements Serializable {

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 创建人 ID（登录态下的管理员 / 用户）；无登录态的写入（如小程序首次自动注册）为 null */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;

    /** 最后修改人 ID，每次 update 由 MetaObjectHandler 覆盖写入 */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    /**
     * 乐观锁版本号。
     * updateById 时 MyBatis-Plus 会把它拼进 where 条件（version = ?）并自动 +1，
     * 受影响行数为 0 说明期间已被别人改过，业务层据此提示「数据已被修改」。
     */
    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    private Integer version;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    @TableField(value = "deleted", select = false)
    private Integer deleted;
}
