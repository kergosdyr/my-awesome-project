package challenge.commerce.infra.db;

import challenge.commerce.domain.payment.PaymentRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    private final PaymentJpaRepository paymentJpaRepository;

    public PaymentRepositoryImpl(PaymentJpaRepository paymentJpaRepository) {
        this.paymentJpaRepository = paymentJpaRepository;
    }

    @Override
    public Optional<PaymentEntity> findByOrderId(long id) {
        return paymentJpaRepository.findByOrderId(id);
    }

    @Override
    public List<PaymentEntity> findByOrderIds(Collection<Long> orderIds) {
        return paymentJpaRepository.findByOrderIdIn(orderIds);
    }

    @Override
    public PaymentEntity create(PaymentEntity payment) {
        return paymentJpaRepository.save(payment);
    }
}
