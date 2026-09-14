package challenge.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HTTP·다른 진입점에서 공통으로 사용할 로컬 결제 결과 조회. PG 재조회/승인을 일으키지 않는다. */
@Service
public class PaymentQueryService {
    private final PaymentReader reader;

    public PaymentQueryService(PaymentReader reader) {
        this.reader = reader;
    }

    @Transactional(readOnly = true)
    public PaymentResult find(long reservationId) {
        return reader.readByReservationId(reservationId)
                .map(payment -> new PaymentResult(payment.getStatus(), payment.getApprovalId()))
                .orElseGet(() -> new PaymentResult(PaymentResult.Status.NOT_FOUND, null));
    }
}
