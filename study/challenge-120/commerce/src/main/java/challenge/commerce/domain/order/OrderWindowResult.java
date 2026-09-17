package challenge.commerce.domain.order;

import java.util.List;

/** B009: 다음 요청에 전달할 표식은 직접 설계한다. 더 읽을 주문이 없으면 next=null. */
public record OrderWindowResult(List<OrderDetailsResult> entries, String next) {}
