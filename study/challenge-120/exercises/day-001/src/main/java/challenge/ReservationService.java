package challenge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Day 001 · B001 — 마지막 예약 한 자리 (Practical 25분)
 *
 * <h2>상황</h2>
 * Spring Boot 인스턴스 2개가 MySQL 8 한 대에 접근한다.
 * 인증된 userId가 주어지며, 취소·결제·캐시·메시지 브로커는 범위 밖이다.
 * <pre>
 * event(event_id PK, remaining INT NOT NULL)
 * reservation(reservation_id PK, event_id, user_id)
 * </pre>
 * remaining은 예약 가능한 남은 자리 수다. 초기 스키마는 resources/schema.sql에 있다.
 *
 * <h2>요구사항</h2>
 * <ul>
 *   <li>같은 이벤트에 한 사용자는 예약을 최대 1건 가진다.</li>
 *   <li>성공하면 예약 1건 생성과 remaining 1 감소가 함께 반영된다.</li>
 *   <li>실패하면 두 변경 모두 남지 않는다. remaining은 음수가 될 수 없다.</li>
 *   <li>기존 예약의 재요청은 기존 예약을 반환하고 추가 차감하지 않는다.</li>
 *   <li>매진된 이후의 기존 사용자 재요청도 기존 예약을 반환한다.</li>
 * </ul>
 *
 * <h2>반환·예외 계약</h2>
 * <ul>
 *   <li>CREATED: 새 예약 ID.</li>
 *   <li>EXISTING: 기존 예약 ID.</li>
 *   <li>SOLD_OUT: reservationId는 null.</li>
 *   <li>DB 저장 장애: DataAccessException을 전달하고 변경은 남기지 않는다.</li>
 *   <li>오늘 테스트의 이벤트는 존재하고 userId는 양수다.</li>
 * </ul>
 *
 * <h2>구현 및 실행</h2>
 * JPA Repository와 기본 @Transactional 경계가 제공된다. 예약 로직과 결과·예외 처리를 구현한다.
 * 필요하면 schema.sql의 제약·인덱스 및 클래스를 변경할 수 있다.
 * IntelliJ에서 {@code Day 001 - Backend}를 선택하고 ▶ 실행.
 * 공개 테스트는 정상·매진·재요청·동시 요청·DB 저장 실패를 검사한다.
 * 테스트 기대값을 바꾸거나 테스트 전용 사용자 ID를 특별 취급하지 않는다.
 * 기본 테스트는 H2와 같은 JVM의 두 Spring 컨텍스트를 사용한다.
 * H2 통과는 실제 MySQL 및 서로 다른 서버에서의 동시성 검증과 구분한다.
 * MySQL 실행 명령은 실습 README.md에 있다.
 * 제출: 코드·SQL·스키마 변경, 본인이 추가한 테스트와 결과, 선택 이유 및 검증 한계.
 *
 * <h2>현재 이어서 풀 부분: 낙관적 락 충돌 이후의 응답</h2>
 * @Version 충돌 감지는 확인됐다. 다음 목표는 충돌한 요청도 서비스 계약에 맞게 처리하는 것이다.
 * 먼저 마지막 1자리 테스트에서 성공 1건과 나머지 SOLD_OUT, 예약 1행·remaining 0을 확인한다.
 * 이어 동일 사용자 재요청에서는 CREATED 1건과 나머지 EXISTING, 동일 예약 ID·차감 1회를 확인한다.
 * 모든 충돌을 SOLD_OUT으로 바꿔도 두 시나리오가 모두 맞는지 먼저 설명해 본다.
 * 구현 방법은 직접 선택하고, 기존 정상·재요청·저장 실패 원복 테스트도 유지한다.
 *
 * <h2>같은 사례로 이어가는 벤치마크</h2>
 * <ul>
 *   <li>Design 15분: 이벤트 하나에 예약 시도 초당 2,000건, 잔여 조회 초당
 *       10,000건. 조회 지연은 최대 3초 허용하나 초과 예약은 불가하다.
 *       요청 흐름, 병목 후보, 대안 두 개와 선택 근거를 설명한다.</li>
 *   <li>Failure 10분: remaining=1에서 서로 다른 두 사용자에게 201 응답.
 *       reservation은 2행 증가했고 remaining=0이며 오류 로그는 없다.
 *       가능한 실행 순서, 판별할 증거, 재현 방법, 먼저 확인할 지점을 설명한다.</li>
 *   <li>Project 10분: 실제 프로젝트의 어려웠던 기술적 결정, 본인 기여,
 *       대안, 결과 근거, 다시 한다면 바꿀 점을 설명한다. 수치는 만들지 않는다.</li>
 *   <li>Explanation 5분: 자료를 닫고 작성한 예약 처리에서 트랜잭션이 보장하는 것과
 *       별도로 확인할 것을 동료에게 5~8문장으로 설명한다.</li>
 * </ul>
 * 설계·설명 답안은 이 파일 아래 주석 또는 별도 메모에 작성해도 된다.
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
