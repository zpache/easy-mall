package cn.easykits.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端消息（api 端只写不读）。
 * <p>
 * 小程序侧的业务事件（下单支付、申请售后、提交评价）在这里落一条消息，
 * 由管理后台的铃铛消费；已读是全局状态，落在 is_read 上。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class Message extends BaseEntity {

    public static final String TYPE_ORDER_PAID = "ORDER_PAID";
    public static final String TYPE_REFUND_APPLY = "REFUND_APPLY";
    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_STOCK_WARN = "STOCK_WARN";

    public static final String LEVEL_INFO = "INFO";
    public static final String LEVEL_WARN = "WARN";
    public static final String LEVEL_URGENT = "URGENT";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String type;

    private String level;

    private String title;

    private String content;

    private String bizType;

    private Long bizId;

    private String link;

    /** 幂等键，重复事件由唯一索引挡住 */
    private String dedupKey;
}
