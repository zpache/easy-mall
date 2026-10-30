package com.easymall.admin.modules.order.vo;

import java.util.List;

/**
 * 物流轨迹（快递100 实时查询结果）。
 */
public record LogisticsTraceVO(
        String company,
        String companyCode,
        String trackingNo,
        /** 快递100 状态：200 正常返回 */
        String status,
        String message,
        /** 轨迹是否已签收 */
        Boolean isChecked,
        /** 签收状态：0 在途，1 揽收，2 疑难，3 签收，4 退签，5 派件，6 退回等 */
        String state,
        List<Node> traces) {

    public record Node(
            /** 原始时间戳（秒） */
            String time,
            /** 格式化时间（快递100 返回，如 2026-10-31 12:30:00） */
            String ftime,
            String context,
            String location) {
    }
}
