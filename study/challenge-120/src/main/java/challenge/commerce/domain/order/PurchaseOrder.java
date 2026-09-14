package challenge.commerce.domain.order;

import java.time.Instant;

/** 주문 당시 상품명·옵션·가격을 기록한다. 결제 상태는 별도의 Payment에서 조회한다. */
public record PurchaseOrder(
        Long id,
        long optionId,
        String productName,
        String optionName,
        String image,
        long unitPrice,
        int quantity,
        long totalAmount,
        Instant createdAt) {}
