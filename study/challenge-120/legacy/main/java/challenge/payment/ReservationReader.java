package challenge.payment;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 결제 유스케이스가 예약 저장소의 기술 API에 직접 의존하지 않도록 하는 조회 경계. */
@Component
@Transactional(readOnly = true)
public class ReservationReader {
    private final ReservationRepository reservations;

    public ReservationReader(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    public Optional<Reservation> readById(long id) {
        return reservations.findById(id);
    }
}
