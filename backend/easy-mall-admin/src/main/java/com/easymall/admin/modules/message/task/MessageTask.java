package com.easymall.admin.modules.message.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.admin.entity.Goods;
import com.easymall.admin.entity.Message;
import com.easymall.admin.mapper.GoodsMapper;
import com.easymall.admin.modules.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消息相关的定时任务：库存预警扫描 + 过期消息清理。
 * <p>
 * 库存预警从「前端每次拉取算个数」改成「后端定时生成事件消息」：
 * 前端铃铛只认未读消息，不再把库存数量当成未读数，确认过的提醒不会因为刷新页面又冒出来。
 * <p>
 * 节奏：每 5 分钟扫一次，库存跌破预警值后最迟 5 分钟就能在铃铛里看到；
 * 同一商品 24 小时内最多提醒一条，避免频繁扫描把消息列表刷爆。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageTask {

    /** 消息保留天数，超过后连同已读记录一起清理 */
    private static final int KEEP_DAYS = 180;
    /** 单次扫描最多处理的商品数，避免极端情况一次插入过多 */
    private static final int SCAN_LIMIT = 200;
    /** 同一商品的库存预警重复提醒间隔（小时）：补货没跟上时每天提醒一次，不刷屏 */
    private static final int WARN_REPEAT_HOURS = 24;

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final GoodsMapper goodsMapper;
    private final MessageService messageService;

    /** 每 5 分钟扫描一次在售商品，把低于预警值的库存写成消息 */
    @Scheduled(cron = "0 */5 * * * ?")
    public void scanStockWarn() {
        scan("定时");
    }

    /** 启动完成后先扫一次，重启后不用等第一个 5 分钟才有预警 */
    @EventListener(ApplicationReadyEvent.class)
    public void scanStockWarnOnStartup() {
        scan("启动");
    }

    private void scan(String trigger) {
        try {
            doScanStockWarn(trigger);
        } catch (Exception e) {
            // 定时任务抛异常会中断本轮，记录后等下一轮重试，不影响其它定时任务
            log.error("库存预警扫描失败（{}触发）", trigger, e);
        }
    }

    private void doScanStockWarn(String trigger) {
        List<Goods> lowStockGoods = goodsMapper.selectList(Wrappers.<Goods>lambdaQuery()
                .eq(Goods::getStatus, 1)
                .gt(Goods::getStockWarn, 0)
                .apply("stock <= stock_warn")
                .orderByAsc(Goods::getStock)
                .last("limit " + SCAN_LIMIT));

        Set<Long> lowIds = lowStockGoods.stream().map(Goods::getId).collect(Collectors.toSet());

        // 1. 库存已补足（或商品已下架）的旧预警自动消除，铃铛里不留假警报
        int resolved = messageService.autoResolve(Message.TYPE_STOCK_WARN, lowIds);
        if (resolved > 0) {
            log.info("库存预警自动消除 {} 条：对应商品库存已恢复", resolved);
        }
        if (lowStockGoods.isEmpty()) {
            return;
        }

        // 2. 24 小时内提醒过、或还挂着未读的，本轮跳过
        Set<Long> notified = messageService.notifiedBizIds(
                Message.TYPE_STOCK_WARN, LocalDateTime.now().minusHours(WARN_REPEAT_HOURS));

        String stamp = LocalDateTime.now().format(STAMP);
        int created = 0;
        for (Goods goods : lowStockGoods) {
            if (notified.contains(goods.getId())) {
                continue;
            }
            String unit = StringUtils.hasText(goods.getUnit()) ? goods.getUnit() : "件";
            int stock = goods.getStock() == null ? 0 : goods.getStock();
            boolean soldOut = stock <= 0;
            boolean inserted = messageService.publish(
                    Message.TYPE_STOCK_WARN,
                    soldOut ? Message.LEVEL_URGENT : Message.LEVEL_WARN,
                    soldOut ? "「" + goods.getName() + "」已售罄" : "「" + goods.getName() + "」库存不足",
                    "当前库存 " + stock + unit + "，预警值 " + goods.getStockWarn() + unit
                            + (soldOut ? "，已无货可发，请尽快补货" : "，请及时补货"),
                    "goods",
                    goods.getId(),
                    "/goods/stock",
                    // 真正的去重靠上面的 24 小时窗口；这里带时间戳只是满足 dedup_key 唯一索引
                    Message.TYPE_STOCK_WARN + ":" + goods.getId() + ":" + stamp
            );
            if (inserted) {
                created++;
            }
        }
        if (created > 0) {
            log.info("库存预警扫描（{}触发）：低库存商品 {} 个，新增消息 {} 条",
                    trigger, lowStockGoods.size(), created);
        }
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
