package challenge.payment;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByReservationId(Long reservationId);
    // TODO 필요한 조회 선언을 추가하세요.
}
