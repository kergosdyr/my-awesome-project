package challenge.commerce.domain.payment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 업무 저장소 계약. 저장 기술은 infra.db에 있으며 이 인터페이스는 JpaRepository를 상속하지 않는다. */
public interface PaymentRepository {
    Optional<Payment> findByOrderId(long orderId);

    List<Payment> findByOrderIds(Collection<Long> orderIds);

    Payment create(Payment payment);

    void update(Payment payment);
}
