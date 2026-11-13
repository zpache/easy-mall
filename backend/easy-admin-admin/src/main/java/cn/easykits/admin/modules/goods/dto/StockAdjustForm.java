package cn.easykits.admin.modules.goods.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 库存调整表单。
 * <p>
 * type = in / out 时 qty 表示增减数量（正数）；
 * type = check 时 qty 表示盘点后的目标库存（可以为 0）。
 */
public record StockAdjustForm(
        @NotNull(message = "请选择商品")
        Long goodsId,

        /** SKU ID：多规格商品必填，按 SKU 维度调整库存；单规格商品不传 */
        Long skuId,

        @NotBlank(message = "请选择调整类型")
        String type,

        @NotNull(message = "数量不能为空")
        @Min(value = 0, message = "数量不能小于 0")
        Integer qty,

        @Size(max = 255, message = "备注过长")
        String remark
) {
}
