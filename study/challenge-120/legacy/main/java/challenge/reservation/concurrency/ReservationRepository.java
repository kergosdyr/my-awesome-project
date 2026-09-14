package challenge.reservation.concurrency;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByEventIdAndId(Long eventId, Long id);
    Optional<Reservation> findByUserIdAndEventId(Long userId, long eventId);
}
