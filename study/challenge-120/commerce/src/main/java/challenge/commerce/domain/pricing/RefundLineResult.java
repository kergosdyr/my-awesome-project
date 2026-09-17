package challenge.commerce.domain.pricing;

import java.util.List;

/** 개별 상품을 취소할 때 돌려줄 금액. 리스트 위치는 해당 품목 안의 고정된 상품 번호다. */
public record RefundLineResult(long optionId, List<Long> unitRefundAmounts) {
    public RefundLineResult {
        unitRefundAmounts = List.copyOf(unitRefundAmounts);
    }
}
