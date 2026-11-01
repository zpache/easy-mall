package com.easymall.admin.modules.order.vo;

import java.util.List;

/**
 * 批量发货结果：成功条数 + 逐条失败原因。
 */
public record BatchDeliverVO(
        int total,
        int successCount,
        List<Failure> failures) {

    public record Failure(String orderNo, String reason) {
    }
}
