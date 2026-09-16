package challenge.commerce.domain.payment;

import challenge.commerce.infra.db.PaymentEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 업무 저장소 계약. 저장 기술은 infra.db에 있으며 이 인터페이스는 JpaRepository를 상속하지 않는다. */
public interface PaymentRepository {
    Optional<PaymentEntity> findByOrderId(long orderId);

    List<PaymentEntity> findByOrderIds(Collection<Long> orderIds);

    PaymentEntity create(PaymentEntity payment);
}
