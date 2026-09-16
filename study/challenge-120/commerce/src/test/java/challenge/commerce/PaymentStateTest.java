package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import challenge.commerce.infra.db.PaymentEntity;
import org.junit.jupiter.api.Test;

class PaymentStateTest {
    @Test
    void approvalAndRepeatPreservePaymentIdentity() {
        var payment = new PaymentEntity(10L, 20, "20", 129000, PaymentEntity.Status.PENDING, null);
        payment.confirmApproval("approval-20");
        payment.confirmApproval("approval-20");
        assertTrue(payment.isPaid());
        assertEquals("approval-20", payment.approvalId());
        assertEquals(10L, payment.id());
        assertEquals(20L, payment.orderId());
        assertEquals("20", payment.key());
        assertEquals(129000, payment.amount());
    }
}
