package challenge.commerce.infra.db;

import challenge.commerce.domain.payment.PaymentRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    private final PaymentJpaRepository payments;

    public PaymentRepositoryImpl(PaymentJpaRepository payments) {
        this.payments = payments;
    }

    @Override
    public Optional<PaymentEntity> findByOrderId(long id) {
        return payments.findByOrderId(id);
    }

    @Override
    public List<PaymentEntity> findByOrderIds(Collection<Long> orderIds) {
        return payments.findByOrderIdIn(orderIds);
    }

    @Override
    public PaymentEntity create(PaymentEntity payment) {
        return payments.save(payment);
    }
}
