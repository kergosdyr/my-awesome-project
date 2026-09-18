package challenge.commerce.domain.payment;

import challenge.commerce.infra.db.PaymentEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PaymentSaver {
    private final PaymentRepository paymentRepository;

    public PaymentSaver(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentEntity create(long orderId, String key, long amount, PaymentEntity.Status status, String approvalId) {
        return paymentRepository.create(new PaymentEntity(null, orderId, key, amount, status, approvalId));
    }
}
