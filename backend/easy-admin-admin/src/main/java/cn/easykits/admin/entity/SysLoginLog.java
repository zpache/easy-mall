package cn.easykits.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理端登录日志：成功 / 失败 / 触发防爆破锁定。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private Long userId;

    private String ip;

    private String userAgent;

    /** 1 成功 0 失败 */
    private Integer success;

    /** 说明：失败原因 / 锁定提示 */
    private String message;

    private LocalDateTime createdAt;
}
