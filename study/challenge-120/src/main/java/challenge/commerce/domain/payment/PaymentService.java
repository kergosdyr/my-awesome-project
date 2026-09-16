package challenge.commerce.domain.payment;

import challenge.commerce.domain.order.OrderReader;
import challenge.commerce.infra.db.PaymentEntity;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Day6: 실제 주문으로 결제한다. 기존 사용자의 pay 정책을 이식했다. 금액은 클라이언트 입력이 아니라 OrderReader로 읽은 주문 스냅샷에서 가져온다. 정상·응답
 * 유실 직후 lookup 정책은 유지한다. 새 과제는 onNotification과 PaymentEntity 상태 전이다. 설계 질문: 어떤 결제를 읽는가? 상태 규칙은 누가 소유하는가?
 * 반복 알림에서 무엇을 보존하는가? 거래는 동일 주문/key/금액·순차 호출이며, 알림은 유효한 APPROVED receipt다. 없는 결제 알림은 false/DB 무변경.
 * 기존 결제는 같은 행/승인ID를 유지하며 true를 반환한다. callback/flush/latch/수동 트랜잭션 장치를 업무 서비스에 넣지 않는다.
 */
@Service
public class PaymentService {
    private final PaymentReader paymentReader;
    private final PaymentSaver paymentSaver;
    private final OrderReader orderReader;
    private final PaymentGateway pg;

    public PaymentService(
            PaymentReader paymentReader, PaymentSaver paymentSaver, OrderReader orderReader, PaymentGateway pg) {
        this.paymentReader = paymentReader;
        this.paymentSaver = paymentSaver;
        this.orderReader = orderReader;
        this.pg = pg;
    }

    @Transactional
    public PaymentResult pay(long orderId) {
        var order = orderReader.read(orderId);
        long amount = order.totalAmount();
        var optionalPayment = paymentReader.readByOrderId(orderId);
        if (optionalPayment.isPresent()) {
            return optionalPayment
                    .filter(PaymentEntity::isPaid)
                    .map(payment -> new PaymentResult(PaymentResult.Status.PAID, payment.approvalId()))
                    .orElseGet(
                            () -> pg.lookup(String.valueOf(orderId)).filter(PaymentGateway.Receipt::approved).stream()
                                    .peek(lookupReceipt -> paymentSaver.create(
                                            orderId,
                                            lookupReceipt.key(),
                                            amount,
                                            PaymentEntity.Status.PAID,
                                            lookupReceipt.approvalId()))
                                    .map(lookupReceipt ->
                                            new PaymentResult(PaymentResult.Status.PAID, lookupReceipt.approvalId()))
                                    .findAny()
                                    .orElseGet(() -> new PaymentResult(PaymentResult.Status.PENDING, null)));
        }

        PaymentGateway.Receipt receipt;
        try {
            receipt = pg.approve(String.valueOf(orderId), orderId, amount);
        } catch (PaymentGateway.ResponseLostException e) {
            return pg.lookup(String.valueOf(orderId)).filter(PaymentGateway.Receipt::approved).stream()
                    .peek(lookupReceipt -> paymentSaver.create(
                            orderId,
                            lookupReceipt.key(),
                            amount,
                            PaymentEntity.Status.PAID,
                            lookupReceipt.approvalId()))
                    .map(lookupReceipt -> new PaymentResult(PaymentResult.Status.PAID, lookupReceipt.approvalId()))
                    .findAny()
                    .orElseGet(() -> new PaymentResult(PaymentResult.Status.PENDING, null));
        }

        if (!receipt.approved()) {
            paymentSaver.create(orderId, receipt.key(), amount, PaymentEntity.Status.PENDING, receipt.approvalId());
            return new PaymentResult(PaymentResult.Status.PENDING, receipt.approvalId());
        }

        paymentSaver.create(orderId, receipt.key(), amount, PaymentEntity.Status.PAID, receipt.approvalId());
        return new PaymentResult(PaymentResult.Status.PAID, receipt.approvalId());
    }

    // 이전 사용자 생성 호출의 인자 순서·반환 상태를 연결하는 이식 경계다.

    /** TODO B006: 기존 사용자 작성본의 새 행 생성 동작을 보존했다. 기존 결제와 PaymentEntity 상태 동작을 조합하도록 개선한다. */
    @Transactional
    public boolean onNotification(PaymentGateway.Receipt receipt) {

        Optional<PaymentEntity> optionalPayment = paymentReader.readByOrderId(receipt.orderId());
        if (optionalPayment.isEmpty()) {
            return false;
        }

        PaymentEntity payment = optionalPayment.get();
        if (receipt.approved()) {
            payment.confirmApproval(receipt.approvalId());
        }
        return true;
    }
}
