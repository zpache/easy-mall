package cn.easykits.api.modules.order.vo;

import cn.easykits.api.entity.OrderRefund;
import cn.easykits.api.entity.MallOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的售后记录（列表与详情共用），含进度所需的时间与审核信息。
 */
public record RefundRecordVO(
        Long id,
        String refundNo,
        Long orderId,
        String orderNo,
        BigDecimal amount,
        BigDecimal orderPayAmount,
        String type,
        String typeText,
        String reason,
        Integer status,
        String statusText,
        String auditRemark,
        LocalDateTime appliedAt,
        LocalDateTime handledAt,
        /** 微信退款单号，到账后可凭此在微信支付账单核对 */
        String wxRefundNo
) {

    public static RefundRecordVO of(OrderRefund refund, MallOrder order) {
        BigDecimal payAmount = order == null || order.getPayAmount() == null
                ? BigDecimal.ZERO
                : order.getPayAmount();
        return new RefundRecordVO(
                refund.getId(),
                refund.getRefundNo(),
                refund.getOrderId(),
                refund.getOrderNo(),
                refund.getAmount(),
                payAmount,
                refund.getType(),
                typeText(refund.getType()),
                refund.getReason(),
                refund.getStatus(),
                RefundApplyVO.statusText(refund.getStatus()),
                refund.getAuditRemark(),
                refund.getAppliedAt(),
                refund.getHandledAt(),
                refund.getWxRefundNo()
        );
    }

    public static String typeText(String type) {
        return "return".equals(type) ? "退货退款" : "仅退款";
    }
}
