package challenge.reservation.basic;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByUserIdAndEventId(Long userId, long eventId);
    // 필요한 조회 메서드는 직접 추가하세요.
}
