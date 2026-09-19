package challenge.commerce.domain.order;

import challenge.commerce.infra.db.OrderEntity;
import challenge.commerce.infra.db.PaymentEntity;

/** 조회한 Entity를 그대로 묶는다. 결제 전에는 payment가 null이다. HTTP 표현은 API에서 결정한다. */
public record OrderDetailsResult(OrderEntity order, PaymentEntity payment) {}
