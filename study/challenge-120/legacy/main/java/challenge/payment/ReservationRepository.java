package challenge.payment;

import org.springframework.data.jpa.repository.JpaRepository;

/** 제공 fixture 조회용. 예약 업무를 새로 구현할 필요 없다. */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {}
