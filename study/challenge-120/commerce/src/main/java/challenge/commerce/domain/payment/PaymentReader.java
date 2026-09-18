package challenge.commerce.domain.payment;

import challenge.commerce.infra.db.PaymentEntity;
import java.util.Collection;
import java.util.List;
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

    public List<PaymentEntity> readForOrders(Collection<Long> orderIds) {
        return orderIds.isEmpty() ? List.of() : paymentRepository.findByOrderIds(orderIds);
    }

    public Optional<PaymentEntity> readByOrderId(long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}
