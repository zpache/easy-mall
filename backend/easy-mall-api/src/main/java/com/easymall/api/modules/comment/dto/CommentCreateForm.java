package com.easymall.api.modules.comment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 提交评价：一次可提交订单内多个商品，每个商品一条评价。
 */
public record CommentCreateForm(
        @NotNull(message = "订单不能为空") Long orderId,
        @NotEmpty(message = "请选择要评价的商品") @Valid List<Item> items
) {

    /**
     * 单个商品的评价内容。images 传晒图的完整访问地址，最多 3 张。
     */
    public record Item(
            @NotNull(message = "商品不能为空") Long goodsId,
            @NotNull(message = "请选择评分") @Min(value = 1, message = "评分最低 1 星")
            @Max(value = 5, message = "评分最高 5 星") Integer score,
            @Size(max = 500, message = "评价内容最多 500 字") String content,
            List<String> images
    ) {
    }
}
