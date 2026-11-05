package com.easymall.admin.modules.message.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.admin.entity.Goods;
import com.easymall.admin.entity.Message;
import com.easymall.admin.mapper.GoodsMapper;
import com.easymall.admin.modules.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 消息相关的定时任务：库存预警扫描 + 过期消息清理。
 * <p>
 * 库存预警从「前端每次拉取算个数」改成「后端生成事件消息」：
 * 前端铃铛只认未读消息，不再把库存数量当成未读数，
 * 这样确认过的提醒不会因为刷新页面又冒出来。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageTask {

    /** 消息保留天数，超过后连同已读记录一起清理 */
    private static final int KEEP_DAYS = 180;
    /** 单次扫描最多处理的商品数，避免极端情况一次插入过多 */
    private static final int SCAN_LIMIT = 200;

    private final GoodsMapper goodsMapper;
    private final MessageService messageService;

    /**
     * 每小时第 5 分钟扫描一次在售的低库存商品。
     * 同一商品同一天只提醒一次（dedup_key 带日期），库存没补上也不会每小时刷屏。
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void scanStockWarn() {
        List<Goods> lowStockGoods = goodsMapper.selectList(Wrappers.<Goods>lambdaQuery()
                .eq(Goods::getStatus, 1)
                .gt(Goods::getStockWarn, 0)
                .apply("stock <= stock_warn")
                .orderByAsc(Goods::getStock)
                .last("limit " + SCAN_LIMIT));
        if (lowStockGoods.isEmpty()) {
            return;
        }

        String today = LocalDate.now().toString();
        for (Goods goods : lowStockGoods) {
            String unit = goods.getUnit() == null ? "件" : goods.getUnit();
            messageService.publish(
                    Message.TYPE_STOCK_WARN,
                    Message.LEVEL_WARN,
                    "「" + goods.getName() + "」库存不足",
                    "当前库存 " + goods.getStock() + unit + "，低于预警值 " + goods.getStockWarn() + unit,
                    "goods",
                    goods.getId(),
                    "/goods/stock",
                    Message.TYPE_STOCK_WARN + ":" + goods.getId() + ":" + today
            );
        }
        log.debug("库存预警扫描完成，低库存商品 {} 个", lowStockGoods.size());
    }

    /** 每天 03:20 清理过期消息，避免表无限增长 */
    @Scheduled(cron = "0 20 3 * * ?")
    public void cleanExpired() {
        int removed = messageService.cleanExpired(KEEP_DAYS);
        if (removed > 0) {
            log.info("清理过期消息 {} 条（保留 {} 天）", removed, KEEP_DAYS);
        }
    }
}
