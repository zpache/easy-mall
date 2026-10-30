package com.easymall.api.modules.member.task;

import com.easymall.api.modules.member.service.MemberPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 会员积分补发。
 *
 * <p>支付成功后会在业务事务里直接发放；若那次发放异常（DB 抖动等），
 * 订单的 points_awarded 仍为 0，由本任务兜底补发，避免用户「付了钱没积分」。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPointTask {

    private final MemberPointService memberPointService;

    /** 每 5 分钟扫一次未发放的红单，单次最多 50 笔 */
    @Scheduled(cron = "0 */5 * * * ?")
    public void repairUnawarded() {
        try {
            int awarded = memberPointService.repairUnawarded();
            if (awarded > 0) {
                log.info("会员积分补发完成，本次补发 {} 笔订单", awarded);
            }
        } catch (Exception e) {
            log.error("会员积分补发任务执行失败", e);
        }
    }
}
