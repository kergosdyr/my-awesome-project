package challenge;

import static challenge.CancellationService.Result.CANCELLED;
import static challenge.CancellationService.Result.NOT_FOUND;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * B002 — 예약 취소와 좌석 반환. 제한시간 45분(Minimum 20분).
 * Day 1의 예약 생성과 같은 모델에서 취소 진입점 하나만 구현한다. 생성 흐름 구현은 오늘 범위 밖.
 * cancel(eventId, reservationId): 해당 이벤트의 예약이 있으면 행을 삭제하고 잔여 좌석을 1 늘려 CANCELLED.
 * 이미 삭제됐거나 예약 ID가 없거나 다른 이벤트의 예약이면 NOT_FOUND, DB 변화 없음.
 * eventId는 존재하는 양수, reservationId는 양수. 예약은 좌석 1개이며 사용자/권한 검사는 범위 밖.
 * 예: event 1 잔여 0, reservation 10이 event 1 소속이면 cancel(1,10) -> CANCELLED, 잔여 1.
 * 같은 호출을 재시도하면 NOT_FOUND, 잔여 1. 반환은 아래 enum이며 DB 장애는 예외를 전파한다.
 * 서로 다른 서비스 인스턴스가 같은 DB에 동시 요청해도 같은 예약의 좌석은 한 번만 반환한다.
 * 같은 이벤트의 서로 다른 예약 두 개 취소는 둘 다 CANCELLED, 잔여 +2가 되어야 한다.
 * DB 저장 실패 시 예약 삭제와 잔여 변경 모두 원복되어야 한다.
 * 엔티티/Repository를 수정해도 되며 업무 로직·조회·동시성 보호 범위는 직접 정한다.
 * 실행 구성: Day 002 - Backend / Day 002 - Backend Minimum.
 * 명령: ./gradlew :day-002:test --tests challenge.CancellationServiceTest --rerun-tasks
 * 제출 설명: 두 취소 요청의 교차 실행 순서, 일관돼야 하는 읽기·쓰기와 트랜잭션의 시작/끝,
 * 선택한 보호가 무엇을 언제까지 보호하는지, DB 저장 실패 시 최종 상태. 대안·비용은 한 문장.
 * 오늘 설명받은 READ→WRITE 용어 재현만으로 이해 점수를 주지 않는다.
 * 공개 테스트는 H2 기능 검사이며 MySQL 락 호환성·실제 성능·모든 스케줄을 증명하지 않는다.
 */
@Service
public class CancellationService {
    public enum Result {CANCELLED, NOT_FOUND}

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;

    public CancellationService(EventRepository events, ReservationRepository reservations) {
        this.eventRepository = events;
        this.reservationRepository = reservations;
    }

    @Transactional
    public Result cancel(long eventId, long reservationId) {
        var events = eventRepository.findByIdWithLock(eventId);
        if (events.isEmpty()) {
            return NOT_FOUND;
        }
        var reservations = reservationRepository.findByEventIdAndId(eventId, reservationId);
        if (reservations.isEmpty()) {
            return NOT_FOUND;
        }
        var event = events.get();
        reservationRepository.deleteAll(reservations);
        event.cancled();
        return CANCELLED;
    }
}
