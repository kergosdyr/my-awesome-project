package challenge.commerce.infra.db;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByOrderIdIn(Collection<Long> orderIds);

    Optional<PaymentEntity> findByOrderId(long orderId);
}
