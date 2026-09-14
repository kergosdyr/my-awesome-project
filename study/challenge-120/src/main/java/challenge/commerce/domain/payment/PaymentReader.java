package challenge.commerce.domain.payment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class PaymentReader {
    private final PaymentRepository payments;

    public PaymentReader(PaymentRepository payments) {
        this.payments = payments;
    }

    public List<Payment> readForOrders(Collection<Long> orderIds) {
        return orderIds.isEmpty() ? List.of() : payments.findByOrderIds(orderIds);
    }

    public Optional<Payment> readByOrderId(long orderId) {
        return payments.findByOrderId(orderId);
    }
}
