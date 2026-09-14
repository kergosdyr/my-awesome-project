package challenge.commerce.domain.payment;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PaymentSaver {
    private final PaymentRepository payments;

    public PaymentSaver(PaymentRepository payments) {
        this.payments = payments;
    }

    public Payment create(
            long orderId, String key, long amount, Payment.Status status, String approvalId) {
        return payments.create(new Payment(null, orderId, key, amount, status, approvalId));
    }

    /** 순수 도메인 객체의 변경 상태를 기존 DB 행에 반영하는 영속화 경계. 새 행을 만들지 않는다. */
    public void saveChanges(Payment payment) {
        payments.update(payment);
    }
}
