package challenge.commerce.api;

import challenge.commerce.api.request.CouponRefundRequest;
import challenge.commerce.api.response.CouponRefundPlanResponse;
import challenge.commerce.domain.pricing.CouponRefundService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** B008 제공 계산 실험. 고객 주문·결제 API의 금액을 변경하지 않는다. */
@RestController
@RequestMapping("/dev/orders/{id}/coupon-refund-plan")
public class CouponRefundExperimentController {
    private final CouponRefundService couponRefundService;

    public CouponRefundExperimentController(CouponRefundService couponRefundService) {
        this.couponRefundService = couponRefundService;
    }

    @PostMapping
    public CouponRefundPlanResponse preview(
            @PathVariable("id") long id, @Valid @RequestBody CouponRefundRequest request) {
        return CouponRefundPlanResponse.from(couponRefundService.preview(id, request.discountAmount()));
    }
}
