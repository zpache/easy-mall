package cn.easykits.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 快递公司：发货下拉与物流轨迹查询的唯一数据源。
 * <p>
 * 以前这份名单硬编码在两个 LogisticsService 和发货弹窗里，加一家快递要改三处、发一次版；
 * 现在落库，由「商城设置 → 快递公司」维护。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_delivery_company")
public class DeliveryCompany extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 展示名：发货时写进订单的 delivery_company */
    private String name;

    /** 快递100 公司编码，如 shunfeng / zhongtong */
    private String code;

    /** 查询轨迹是否必须传收件人手机号后四位：1 是（顺丰、EMS），0 否 */
    private Integer phoneRequired;

    /** 排序，越小越靠前 */
    private Integer sort;

    /** 1 启用，0 停用（停用后不出现在发货下拉） */
    private Integer status;

    private String remark;
}
