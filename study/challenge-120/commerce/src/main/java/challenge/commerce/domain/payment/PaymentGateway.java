package challenge.commerce.domain.payment;

import java.util.Optional;

public interface PaymentGateway {
    enum Status {
        PROCESSING,
        APPROVED
    }

    record Receipt(String key, long orderId, long amount, Status status, String approvalId) {
        public boolean approved() {
            return status == Status.APPROVED;
        }
    }

    Receipt approve(String key, long orderId, long amount);

    Optional<Receipt> lookup(String key);

    class ResponseLostException extends RuntimeException {}
}
