package challenge.commerce.infra.db;

import challenge.commerce.domain.payment.*;
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
    public Optional<Payment> findByOrderId(long id) {
        return payments.findByOrderId(id).map(PaymentEntity::toDomain);
    }

    @Override
    public List<Payment> findByOrderIds(Collection<Long> orderIds) {
        return payments.findByOrderIdIn(orderIds).stream().map(PaymentEntity::toDomain).toList();
    }

    @Override
    public Payment create(Payment payment) {
        return payments.save(PaymentEntity.from(payment)).toDomain();
    }

    @Override
    public void update(Payment payment) {
        payments.findById(payment.id()).orElseThrow().reflect(payment);
    }
}
