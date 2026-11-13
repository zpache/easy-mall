package cn.easykits.admin.modules.goods.vo;

import java.util.List;

/**
 * 批量设置库存结果：成功条数 + 逐条失败原因。
 */
public record BatchStockVO(
        int total,
        int successCount,
        List<Failure> failures) {

    public record Failure(String goodsCode, String goodsName, String reason) {
    }
}
