package cn.easykits.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理端消息：事件型、广播给所有管理员。
 * <p>
 * 已读是<b>全局</b>语义：消息是给整个运营团队看的，谁先处理谁点确认，
 * 这条消息就算办完了，所有人都不再看到它。所以已读状态直接落在消息行上，
 * 不按管理员分别记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class Message extends BaseEntity {

    /** 新订单（已支付待发货） */
    public static final String TYPE_ORDER_PAID = "ORDER_PAID";
    /** 售后申请 */
    public static final String TYPE_REFUND_APPLY = "REFUND_APPLY";
    /** 新评价 */
    public static final String TYPE_COMMENT = "COMMENT";
    /** 库存预警 */
    public static final String TYPE_STOCK_WARN = "STOCK_WARN";
    /** 系统公告 */
    public static final String TYPE_SYSTEM = "SYSTEM";

    public static final String LEVEL_INFO = "INFO";
    public static final String LEVEL_WARN = "WARN";
    public static final String LEVEL_URGENT = "URGENT";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 类型，取值见本类常量 */
    private String type;

    /** 级别：INFO / WARN / URGENT，前端据此上色 */
    private String level;

    private String title;

    private String content;

    /** 业务类型：order / refund / goods / comment */
    private String bizType;

    /** 业务主键，跳转时可定位到具体单据 */
    private Long bizId;

    /** 点击跳转的前端路由 */
    private String link;

    /** 幂等键：同一业务事件只生成一条消息 */
    private String dedupKey;

    /** 是否已读：0 未读 1 已读（全局，任一人确认即已读） */
    private Integer isRead;

    /** 确认人：哪个管理员点的已读，便于追溯「谁处理的」 */
    private Long readBy;

    private LocalDateTime readAt;
}
