package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理后台账号。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** BCrypt 密文，禁止序列化到响应 */
    @JsonIgnore
    private String password;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    /** 所属部门 ID */
    private Long deptId;

    /** 主角色标识，兜底用；优先取 sys_user_role 关联的角色 */
    private String role;

    /** 1 正常，0 禁用 */
    private Integer status;

    private LocalDateTime lastLoginAt;
}
