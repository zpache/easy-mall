package cn.easykits.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 快递公司（只读）：由后台「商城设置 → 快递公司」维护，小程序端物流查询时按名称解析编码。
 */
@Data
@TableName("mall_delivery_company")
public class DeliveryCompany {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 展示名：与订单上的 delivery_company 一致 */
    private String name;

    /** 快递100 公司编码，如 shunfeng */
    private String code;

    /** 查询轨迹是否必须传收件人手机号后四位：1 是 */
    private Integer phoneRequired;

    private Integer sort;

    /** 1 启用 0 停用 */
    private Integer status;

    private String remark;
}
