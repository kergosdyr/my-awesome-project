package challenge.reservation.concurrency;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * B003 · 기존 예약/취소 구현을 이어서 사용하는 혼합 경합 실습.
 * Day 1 ReservationService와 Day 2 CancellationService의 사용자 구현을 시작점으로 재사용한다.
 * 정원1, event=1 remaining=0, reservation=10 user=100에서 reserve(1,200)/cancel(1,10)이 겹친다.
 * 취소 커밋 후 예약 시작: CANCELLED/CREATED, 최종 기존예약0·새예약1·잔여0.
 * 예약 매진 완료 후 취소: SOLD_OUT/CANCELLED, 최종 예약0·잔여1.
 * 겹칠 때 두 결과 모두 허용하지만 응답과 최종 DB 상태가 일치해야 한다. 예약수+잔여=1.
 * 취소 실패·롤백 시 기존예약1·잔여0, 새 예약은 SOLD_OUT. 변경은 요청 단위로 모두 원복한다.
 * 예약 재요청의 EXISTING 및 예약ID 반환, 취소 재요청 NOT_FOUND 등 기존 계약도 유지한다.
 * B003 추가 계약: 없는 이벤트의 예약도 NOT_FOUND, reservationId=null, DB 무변경.
 * ReservationResult에 NOT_FOUND 타입만 준비했다. 해당 분기는 사용자 구현 대상이다.
 * 기존 로직을 다시 작성하거나 일부러 망가뜨릴 필요 없다. 통과하는 구현은 그대로 유지한다.
 * Spring Data JPA 서비스/Repository로 구현하고 동시성 검증·실행 환경은 AI가 담당한다.
 * 사용자에게 EntityManager/flush/테스트 콜백·동기화 도구 구현을 요구하지 않는다.
 * 학습 초점: 두 업무 경로가 함께 실행될 때 응답과 불변식이 유지되는지 판단하기.
 * 시간 Backend45분/Minimum20분, 환경 준비 제외. 추가 설명은 구두/메시지로도 받는다.
 * 실행 구성 reservation-concurrency - Backend / Backend Minimum. 공개 테스트 MixedReservationServiceTest.
 * H2 검사이며 MySQL·성능·모든 스케줄은 미검증. 기본 구현 재사용은 오늘 독립 해결 성과가 아니다.
 */

@Service
public class ReservationService {
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(EventRepository eventRepository, ReservationRepository reservationRepository) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public ReservationResult reserve(long eventId, long userId) {

        var event = eventRepository.findByIdWithLock(eventId).orElseThrow();
        var existingReservations = reservationRepository.findByUserIdAndEventId(userId, eventId);
        if (existingReservations.isPresent()) {
            return new ReservationResult(ReservationResult.Status.EXISTING, existingReservations.get().getId());
        }
        if (event.isFullyReserved()) {
            return new ReservationResult(ReservationResult.Status.SOLD_OUT, null);
        }
        event.reserved();
        var savedReservation = reservationRepository.save(new Reservation(eventId, userId));
        return new ReservationResult(ReservationResult.Status.CREATED, savedReservation.getId());
    }
}
