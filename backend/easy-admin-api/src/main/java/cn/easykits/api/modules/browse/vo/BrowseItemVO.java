package cn.easykits.api.modules.browse.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 浏览足迹列表项：最近浏览时间 + 商品快照。
 */
public record BrowseItemVO(
        Long goodsId,
        String goodsName,
        String mainImage,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer goodsStatus,
        LocalDateTime browsedAt) {
}
