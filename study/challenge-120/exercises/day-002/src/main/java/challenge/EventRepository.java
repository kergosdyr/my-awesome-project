package challenge;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface EventRepository extends JpaRepository<Event, Long> {
    // TODO B002: 필요한 조회·동시성 설정은 직접 추가하세요.

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = ?1")
    Optional<Event> findByIdWithLock(Long id);

}
