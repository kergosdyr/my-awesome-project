package challenge.commerce.domain.payment;

import challenge.commerce.infra.db.PaymentEntity;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class PaymentReader {
    private final PaymentRepository paymentRepository;

    public PaymentReader(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Optional<PaymentEntity> readByOrderId(long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}
