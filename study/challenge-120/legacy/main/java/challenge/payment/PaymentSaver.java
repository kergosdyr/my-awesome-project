package challenge.payment;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 새 결제 생성·영속화 부품. 기존 결제의 상태 전이는 Payment가 담당한다. */
@Component
@Transactional
public class PaymentSaver {
    private final PaymentRepository payments;

    public PaymentSaver(PaymentRepository payments) {
        this.payments = payments;
    }

    public Payment create(
            long reservationId,
            String key,
            String approvalId,
            long amount,
            PaymentResult.Status status) {
        return payments.save(new Payment(reservationId, key, approvalId, amount, status));
    }
}
