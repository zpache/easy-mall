package com.easymall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理端操作日志：AOP 自动采集所有写操作（POST/PUT/DELETE）。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    /** 业务模块（控制器 @Tag name） */
    private String module;

    /** 操作（接口 @Operation summary） */
    private String action;

    private String method;

    private String uri;

    /** 请求参数（序列化截断） */
    private String params;

    /** 响应码：0 成功 */
    private Integer resultCode;

    private String errorMsg;

    private String ip;

    /** 耗时（毫秒） */
    private Integer costMs;

    private LocalDateTime createdAt;
}
