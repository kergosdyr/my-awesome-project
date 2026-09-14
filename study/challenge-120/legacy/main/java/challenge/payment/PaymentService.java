package challenge.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * B006 — 기존 결제 코드에서 이어지는 HTTP 요청·승인 알림 흐름.
 *
 * <p>오늘 해결할 문제: 결제 요청의 PENDING 응답 뒤 PG 승인을 알림으로 받아 같은 결제 결과를 조회한다.
 *
 * <p>왜 중요한가: PG 승인과 우리 DB는 별개이며, 반복 알림이 새 결제를 만들면 상태가 불일치한다.
 *
 * <p>오늘 배울 핵심 개념: HTTP 진입점, Reader/Saver 조합, Payment가 소유하는 상태 전이.
 *
 * <p>완료 기준: 실제 HTTP 실행, 알림 계약 통과, 상태·승인ID 규칙과 각 계층 책임 설명.
 *
 * <p>기존 Day5 pay 판단은 사용자 구현을 유지하고 저장소 접근만 Reader/Saver로 추출했다. Day5 핵심은 동일 예약·금액의 정상 승인/응답 유실/로컬 중단
 * 뒤 재요청이었다. Day6는 POST /payments -> PG PROCESSING -> PG APPROVED -> POST /payments/notifications ->
 * GET /payments/{reservationId}의 흐름을 연결한다. HTTP와 가짜 PG 실행기는 challenge.lab에 있다.
 *
 * <p>오늘 입력은 동일 예약/key/금액의 순차 요청과 유효한 APPROVED 알림이다. PG PROCESSING은 승인 전, APPROVED는 승인 완료. 로컬
 * PENDING은 승인 미확정, PAID는 승인 확인. 알려진 결제의 알림은 true, 같은 승인 반복도 true이며 같은 결제행/승인ID를 유지한다. 로컬 결제가 없으면
 * false이고 새 결제를 생성하지 않는다. 금액변경/위조/PROCESSING 알림, 동시성/환불/자동 재전송/알림 없이 PENDING 재요청 복구는 오늘 신규 평가 밖이다.
 *
 * <p>설계 질문: 어떤 결제를 읽을까? 상태와 승인ID의 규칙은 누가 소유할까? 반복 알림에서 어떤 값을 보존하며 어떤 부품을 조합할까? Service 집중과
 * Reader/Saver·Payment 책임 분리의 재사용 이점/추적 비용을 비교한다.
 *
 * <p>제공: Controller/조회 facade/Reader/Saver/실행 가능한 PG/HTTP 테스트/Spotless. 사용자 구현: 아래 onNotification과
 * Payment의 상태 전이 규칙. 기존 작성본을 TODO로 표시해 보존했다. Backend45분(관찰5/설계8/구현22/실행·설명10), Minimum20분(3/4/8/5).
 * 실제 PG·외부 네트워크 결제·프로세스 재시작 영속성은 검증하지 않는다.
 */
@Service
public class PaymentService {
    private final PaymentReader paymentReader;
    private final PaymentSaver paymentSaver;
    private final ReservationReader reservationReader;
    private final PaymentGateway pg;

    public PaymentService(
            PaymentReader paymentReader,
            PaymentSaver paymentSaver,
            ReservationReader reservationReader,
            PaymentGateway pg) {
        this.paymentReader = paymentReader;
        this.paymentSaver = paymentSaver;
        this.reservationReader = reservationReader;
        this.pg = pg;
    }

    @Transactional
    public PaymentResult pay(long reservationId, long amount) {
        var optionalReservation = reservationReader.readById(reservationId);
        if (optionalReservation.isEmpty()) {
            return new PaymentResult(PaymentResult.Status.NOT_FOUND, null);
        }
        var optionalPayment = paymentReader.readByReservationId(reservationId);
        if (optionalPayment.isPresent()) {
            return optionalPayment
                    .filter(Payment::isPaid)
                    .map(
                            payment ->
                                    new PaymentResult(
                                            PaymentResult.Status.PAID, payment.getApprovalId()))
                    .orElseGet(
                            () ->
                                    pg
                                            .lookup(String.valueOf(reservationId))
                                            .filter(PaymentGateway.Receipt::approved)
                                            .stream()
                                            .peek(
                                                    lookupReceipt ->
                                                            paymentSaver.create(
                                                                    reservationId,
                                                                    lookupReceipt.key(),
                                                                    lookupReceipt.approvalId(),
                                                                    amount,
                                                                    PaymentResult.Status.PAID))
                                            .map(
                                                    lookupReceipt ->
                                                            new PaymentResult(
                                                                    PaymentResult.Status.PAID,
                                                                    lookupReceipt.approvalId()))
                                            .findAny()
                                            .orElseGet(
                                                    () ->
                                                            new PaymentResult(
                                                                    PaymentResult.Status.PENDING,
                                                                    null)));
        }

        PaymentGateway.Receipt receipt;
        try {
            receipt = pg.approve(String.valueOf(reservationId), reservationId, amount);
        } catch (PaymentGateway.ResponseLostException e) {
            return pg
                    .lookup(String.valueOf(reservationId))
                    .filter(PaymentGateway.Receipt::approved)
                    .stream()
                    .peek(
                            lookupReceipt ->
                                    paymentSaver.create(
                                            reservationId,
                                            lookupReceipt.key(),
                                            lookupReceipt.approvalId(),
                                            amount,
                                            PaymentResult.Status.PAID))
                    .map(
                            lookupReceipt ->
                                    new PaymentResult(
                                            PaymentResult.Status.PAID, lookupReceipt.approvalId()))
                    .findAny()
                    .orElseGet(() -> new PaymentResult(PaymentResult.Status.PENDING, null));
        }

        if (!receipt.approved()) {
            paymentSaver.create(
                    reservationId,
                    receipt.key(),
                    receipt.approvalId(),
                    amount,
                    PaymentResult.Status.PENDING);
            return new PaymentResult(PaymentResult.Status.PENDING, receipt.approvalId());
        }

        paymentSaver.create(
                reservationId,
                receipt.key(),
                receipt.approvalId(),
                amount,
                PaymentResult.Status.PAID);
        return new PaymentResult(PaymentResult.Status.PAID, receipt.approvalId());
    }

    /**
     * TODO B006: 아래 사용자 작성 흐름을 보존했다. 새 행 생성 대신 기존 결제의 알림 처리를 설계한다. 오늘 계약: 같은 예약/key/금액의 APPROVED
     * 알림, 중복 알림, 없는 결제 알림. 기존 결제의 상태와 승인 ID를 함께 다루는 규칙은 Payment가 소유한다. Day5의 필수 계약을 확대하는 것이 아니라
     * Day6의 새 평가 대상이다.
     */
    @Transactional
    public boolean onNotification(PaymentGateway.Receipt receipt) {

        paymentSaver.create(
                receipt.reservationId(),
                receipt.key(),
                receipt.approvalId(),
                receipt.amount(),
                PaymentResult.Status.PAID);
        return receipt.approved();
    }
}
