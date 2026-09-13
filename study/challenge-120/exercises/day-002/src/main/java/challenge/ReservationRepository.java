package challenge;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByEventIdAndId(Long eventId, Long id);
    // TODO B002: 필요한 조회·동시성 설정은 직접 추가하세요.
}
