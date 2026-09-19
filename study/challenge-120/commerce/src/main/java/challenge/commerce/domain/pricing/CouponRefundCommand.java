package challenge.commerce.domain.pricing;

import challenge.commerce.support.BusinessException;
import java.util.List;

/** B008 제공 입력 검증. 쿠폰 적용 대상은 주문의 모든 상품이며 배송비는 없다. */
public record CouponRefundCommand(List<CouponLineCommand> lines, long discountAmount) {
    public CouponRefundCommand {
        lines = List.copyOf(lines);
        long total = lines.stream()
                .mapToLong(line -> Math.multiplyExact(line.unitPrice(), line.quantity()))
                .sum();
        if (discountAmount < 0 || discountAmount > total) {
            throw BusinessException.invalid("쿠폰 금액은 0원 이상 상품 합계 이하여야 합니다.");
        }
    }
}
