package com.easymall.api.modules.order.task;

import com.easymall.api.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 发货超时自动确认收货。
 *
 * <p>每 20 分钟扫一次「待收货且发货时间超过 N 天」的订单，自动置为已完成，
 * 天数取自「商城设置 - 支付配置 - 自动确认收货天数」，配 0 即关闭自动完成。</p>
 *
 * <p>更新语句带 status=2 条件，多实例并发或用户同时手动确认时只会成功一次。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoReceiveTask {

    private final OrderService orderService;

    @Scheduled(cron = "0 */20 * * * ?")
    public void autoFinishOrders() {
        try {
            int finished = orderService.autoFinishOrders();
            if (finished > 0) {
                log.info("自动确认收货完成，本次完成 {} 笔订单", finished);
            }
        } catch (Exception e) {
            // 定时任务异常不能中断调度线程，只记录日志等待下一轮
            log.error("自动确认收货任务执行失败", e);
        }
    }
}
