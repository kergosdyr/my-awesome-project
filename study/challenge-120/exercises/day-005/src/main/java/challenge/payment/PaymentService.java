package challenge.payment;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * B005 — 예약 후 결제 결과 확인. 기본 45분, Minimum 20분.
 * Day 5 안의 독립 예약 fixture를 사용한다. 예전 예약 프로젝트와 연결하지 않는다.
 * 목적: 외부 승인이 끝났어도 우리 DB에서는 모를 수 있는 상황을 다룬다.
 * PG의 PROCESSING은 접수 후 승인 전, APPROVED는 승인 완료다.
 * 우리 PENDING은 로컬에서 승인 완료를 확정하지 못한 상태다. 타임아웃 자체를 뜻하지 않는다.
 * PG PROCESSING 응답을 받았을 때와 응답을 잃어 PG 상태를 모를 때 모두 PENDING일 수 있다.
 * 응답 유실은 ResponseLostException으로 재현한다. 가짜 PG는 이 예외 전에 이미 승인한다.
 * 예: 예약1/1000원 요청 → PG APPROVED → 응답 유실 → 로컬 PENDING.
 * 나중에 동일 요청 또는 승인 알림으로 확인 → 로컬 PAID. PG 실제 승인은 1건이어야 한다.
 * 첫 호출에서 즉시 재시도하지 않는 조건은 관찰을 나누기 위한 실습 제약이며 실무의 유일한 정책이 아니다.
 * 여기까지 구현해도 실제 PG 연동·HTTP API·재시작 복구를 갖춘 결제 시스템 완성을 뜻하지 않는다.
 * 입력 pay(reservationId, amount): 이미 만들어진 예약 ID, 양수 원화 금액(long).
 * 결과 PAID(승인 ID), PENDING(null: 아직 확정 못함), NOT_FOUND(null), CONFLICT(null).
 * 1. 없는 예약은 NOT_FOUND, 외부 호출/결제 행 생성 없음.
 * 2. 정상 승인 시 PAID와 승인 ID를 반환하고 DB에도 같은 결과를 남긴다.
 * 3. 승인 후 응답 유실 시 해당 호출은 PENDING을 반환하고 DB에 PENDING을 남긴다.
 *    이 호출 안에서 즉시 조회/재시도하지 않는다. 예약 fixture는 그대로 보존한다.
 * 4. 같은 예약/금액으로 나중에 다시 pay하면 기존 결제를 확인한다.
 *    승인 결과가 있으면 같은 승인 ID로 PAID, 결제사 실제 승인 수와 DB 결제 행은 각 1개다.
 *    아직 PROCESSING이면 PENDING. 다른 금액의 재요청은 CONFLICT, 외부 호출/DB 변경 없음.
 * 5. onNotification(receipt): 알려진 결제의 key/예약/금액이 일치하면 true,
 *    알 수 없거나 불일치하면 false와 변경 없음. 인증은 이미 끝난 알림이다.
 *    APPROVED 알림은 PAID로 반영한다. 중복 APPROVED와 늦은 PROCESSING은 PAID를 되돌리지 않는다.
 *    알림은 새 승인 API를 호출하지 않는다. APPROVED에는 항상 동일한 유효 승인 ID가 있다.
 * 6. 결제 실패/환불/예약 취소·만료와 동시 실행은 오늘 범위 밖. 예약 fixture를 변경하지 않는다.
 * 예: pay -> PENDING(외부는 승인됨), 같은 pay -> PAID, 중복 알림 -> PAID 유지.
 * PaymentGateway API 문서가 제공되며, 필요한 Repository 선언과 업무 본문을 구현한다.
 * @Transactional 기본 제공; 필요하면 설정을 조정할 수 있다. 테스트 장치는 src/test에만 있다.
 * 실행 구성: Day 005 - Backend / Backend Minimum / Backend MySQL.
 * 제출: 코드·테스트, 실제 시간·도움, 응답 유실을 실패로 단정할 수 있는지와 재요청 식별 근거 설명.
 * 설명 질문(같은 시간 안): 외부 승인 후 DB 커밋 자체가 실패하면 오늘 보장 중 무엇이 깨지는가?
 * H2는 순차 계약 확인용. MySQL 선택 검사도 프로세스 사망/네트워크/동시 요청 복구를 보장하지 않는다.
 */
@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentGateway pg;
    public PaymentService(PaymentRepository payments, ReservationRepository reservations, PaymentGateway gateway) {
        this.paymentRepository = payments; this.reservationRepository = reservations; this.pg = gateway;
    }
    @Transactional
    public PaymentResult pay(long reservationId, long amount) {
        var optionalReservation = reservationRepository.findById(reservationId);
        if (optionalReservation.isEmpty()) {
            return new PaymentResult(PaymentResult.Status.NOT_FOUND, null);
        }

        PaymentGateway.Receipt receipt = pg.approve(String.valueOf(reservationId), reservationId, amount);
        // 작성 중이던 줄: receipt.
        throw new UnsupportedOperationException("TODO B005: PG 응답 처리부터 이어서 구현");

    }
    @Transactional
    public boolean onNotification(PaymentGateway.Receipt receipt) {
        throw new UnsupportedOperationException("TODO B005 notification");
    }
}
