package challenge.commerce.domain.order;

import challenge.commerce.infra.db.OrderEntity;
import challenge.commerce.infra.db.PaymentEntity;

/** 주문과 결제를 조합한 조회 결과. HTTP 응답은 api에서 별도로 만든다. */
public record OrderDetailsResult(OrderEntity order, PaymentStatus paymentStatus, String approvalId) {
    public enum PaymentStatus {
        UNPAID,
        PENDING,
        PAID
    }

    public static OrderDetailsResult withoutPayment(OrderEntity order) {
        return new OrderDetailsResult(order, PaymentStatus.UNPAID, null);
    }

    public static OrderDetailsResult withPayment(OrderEntity order, PaymentEntity payment) {
        var status =
                switch (payment.status()) {
                    case PENDING -> PaymentStatus.PENDING;
                    case PAID -> PaymentStatus.PAID;
                };
        return new OrderDetailsResult(order, status, payment.approvalId());
    }
}
