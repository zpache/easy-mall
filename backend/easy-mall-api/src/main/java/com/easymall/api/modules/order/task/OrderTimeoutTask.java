package com.easymall.api.modules.order.task;

import com.easymall.api.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时关单。
 *
 * <p>每分钟扫一次「待付款且已超过支付时限」的订单，关闭并释放库存/优惠券。
 * 关单用带 status 条件的更新，天然幂等：多实例同时跑也只有一个实例能改成功，
 * 不会重复回滚库存。</p>
 *
 * <p>支付时限来自「商城设置 - 支付配置 - 支付超时（分钟）」，后台改完无需重启。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderService orderService;

    @Scheduled(cron = "0 * * * * ?")
    public void closeExpiredOrders() {
        try {
            int closed = orderService.closeExpiredOrders();
            if (closed > 0) {
                log.info("超时关单完成，本次关闭 {} 笔订单", closed);
            }
        } catch (Exception e) {
            // 定时任务异常不能中断调度线程，只记录日志等待下一分钟重试
            log.error("超时关单任务执行失败", e);
        }
    }
}
