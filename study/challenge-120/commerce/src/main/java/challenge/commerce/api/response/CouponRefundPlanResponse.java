package challenge.commerce.api.response;

import challenge.commerce.domain.pricing.CouponRefundPlanResult;
import java.util.List;

public record CouponRefundPlanResponse(List<Line> lines) {
    public record Line(long optionId, List<Long> unitRefundAmounts) {}

    public static CouponRefundPlanResponse from(CouponRefundPlanResult result) {
        return new CouponRefundPlanResponse(result.lines().stream()
                .map(line -> new Line(line.optionId(), line.unitRefundAmounts()))
                .toList());
    }
}
