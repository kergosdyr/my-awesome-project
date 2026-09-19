package challenge.commerce.domain.order;

import java.util.List;

/** B009: next에는 마지막 주문의 시각과 ID를 담는다. 더 읽을 주문이 없으면 next=null. */
public record OrderWindowResult(List<OrderDetailsResult> entries, OrderCursor next) {}
