package challenge.commerce.domain.pricing;

import java.util.List;

/** 품목마다 개별 상품의 환불액을 미리 확정한 계획. 실제 환불을 실행하지 않는다. */
public record CouponRefundPlanResult(List<RefundLineResult> lines) {
    public CouponRefundPlanResult {
        lines = List.copyOf(lines);
    }
}
