package challenge.commerce.domain.pricing;

import challenge.commerce.domain.order.OrderReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 제공 실험 진입점. 실제 주문의 가격 스냅샷으로 가상의 쿠폰·환불 계획을 계산한다. */
@Service
public class CouponRefundService {
    private final OrderReader orderReader;
    private final CouponRefundPlanner couponRefundPlanner;

    public CouponRefundService(OrderReader orderReader, CouponRefundPlanner couponRefundPlanner) {
        this.orderReader = orderReader;
        this.couponRefundPlanner = couponRefundPlanner;
    }

    @Transactional(readOnly = true)
    public CouponRefundPlanResult preview(long orderId, long discountAmount) {
        var order = orderReader.read(orderId);
        var lines = order.items().stream()
                .map(item -> new CouponLineCommand(item.optionId(), item.unitPrice(), item.quantity()))
                .toList();
        return couponRefundPlanner.plan(new CouponRefundCommand(lines, discountAmount));
    }
}
