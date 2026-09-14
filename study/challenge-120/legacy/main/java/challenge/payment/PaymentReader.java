package challenge.payment;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 결제 조회 부품. 결제 요청·결과 조회·알림 처리에서 같은 조회 의미를 재사용한다. */
@Component
@Transactional(readOnly = true)
public class PaymentReader {
    private final PaymentRepository payments;

    public PaymentReader(PaymentRepository payments) {
        this.payments = payments;
    }

    public Optional<Payment> readByReservationId(long reservationId) {
        return payments.findByReservationId(reservationId);
    }
}
