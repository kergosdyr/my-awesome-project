package challenge.payment;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;
import static challenge.payment.PaymentResult.Status.*;

/** 테스트를 트랜잭션으로 감싸지 않고 서비스 반환 후 별도 조회로 DB 상태를 확인한다. */
class PaymentContractTest extends PaymentTestSupport {
    @Autowired PaymentService service;
    @Autowired PaymentRepository payments;
    @Autowired ReservationRepository reservations;
    @Autowired FakePaymentGateway gateway;
    long reservationId;
    @BeforeEach void seed() {
        payments.deleteAll(); reservations.deleteAll(); gateway.reset();
        reservationId = reservations.save(new Reservation(1L)).getId();
    }
    void local(PaymentResult.Status status, String approvalId) {
        var rows = payments.findAll(); assertEquals(1, rows.size()); var payment = rows.getFirst();
        assertEquals(reservationId, payment.getReservationId()); assertEquals(1000, payment.getAmount());
        assertEquals(status, payment.getStatus()); assertEquals(approvalId, payment.getApprovalId());
        assertEquals(gateway.onlyReceipt().key(), payment.getPaymentKey());
        assertEquals(1, reservations.count()); assertTrue(reservations.existsById(reservationId));
    }
    void lost() {
        gateway.mode(FakePaymentGateway.Mode.APPROVE_THEN_LOSE_RESPONSE);
        assertEquals(new PaymentResult(PENDING, null), service.pay(reservationId, 1000));
        assertEquals(1, gateway.approvals()); assertEquals(1, gateway.approveCalls()); assertEquals(0, gateway.lookupCalls());
        local(PENDING, null);
    }
    @Test void normalApprovalAndRepeat() {
        var first = service.pay(reservationId, 1000); assertEquals(PAID, first.status());
        assertNotNull(first.approvalId()); assertEquals(gateway.onlyReceipt().approvalId(), first.approvalId());
        assertEquals(first, service.pay(reservationId, 1000)); assertEquals(1, gateway.approvals()); local(PAID, first.approvalId());
    }
    @Test void approvalResponseLostRemainsPending() { lost(); }
    @Test void laterUserRequestFindsSameApproval() {
        lost(); var result = service.pay(reservationId, 1000);
        assertEquals(new PaymentResult(PAID, gateway.onlyReceipt().approvalId()), result);
        assertEquals(1, gateway.approvals()); local(PAID, result.approvalId());
    }
    @Test void missingReservationDoesNotCallProvider() {
        assertEquals(new PaymentResult(NOT_FOUND, null), service.pay(-1, 1000));
        assertEquals(0, gateway.approveCalls()); assertEquals(0, gateway.lookupCalls()); assertEquals(0, payments.count());
    }
    @Test @Tag("full") void changedAmountConflictsWithoutExternalCall() {
        lost(); int calls = gateway.approveCalls(), lookups = gateway.lookupCalls();
        assertEquals(new PaymentResult(CONFLICT, null), service.pay(reservationId, 2000));
        assertEquals(calls, gateway.approveCalls()); assertEquals(lookups, gateway.lookupCalls()); local(PENDING, null);
    }
    @Test @Tag("full") void duplicateAndLateNotificationCannotRegressPaid() {
        lost(); var receipt = gateway.onlyReceipt(); int calls = gateway.approveCalls();
        assertTrue(service.onNotification(receipt)); assertTrue(service.onNotification(receipt));
        assertTrue(service.onNotification(new PaymentGateway.Receipt(receipt.key(), reservationId, 1000, PaymentGateway.Status.PROCESSING, null)));
        assertEquals(calls, gateway.approveCalls()); assertEquals(1, gateway.approvals()); local(PAID, receipt.approvalId());
    }
    @Test @Tag("full") void unknownAndMismatchedNotificationsAreIgnored() {
        lost(); var receipt = gateway.onlyReceipt();
        assertFalse(service.onNotification(new PaymentGateway.Receipt("unknown", reservationId, 1000, PaymentGateway.Status.APPROVED, "other")));
        assertFalse(service.onNotification(new PaymentGateway.Receipt(receipt.key(), reservationId, 2000, PaymentGateway.Status.APPROVED, receipt.approvalId())));
        assertFalse(service.onNotification(new PaymentGateway.Receipt(receipt.key(), reservationId + 1, 1000, PaymentGateway.Status.APPROVED, receipt.approvalId())));
        assertEquals(1, gateway.approveCalls()); local(PENDING, null);
    }
    @Test @Tag("full") void processingThenApprovalNotification() {
        gateway.mode(FakePaymentGateway.Mode.PROCESSING);
        assertEquals(new PaymentResult(PENDING, null), service.pay(reservationId, 1000));
        assertEquals(new PaymentResult(PENDING, null), service.pay(reservationId, 1000)); local(PENDING, null);
        assertEquals(0, gateway.approvals()); var approved = gateway.complete(gateway.onlyReceipt().key());
        assertTrue(service.onNotification(approved)); assertEquals(1, gateway.approvals()); local(PAID, approved.approvalId());
    }
    @Test @Tag("full") void differentReservationsHaveDifferentPayments() {
        var first = service.pay(reservationId, 1000);
        long secondId = reservations.save(new Reservation(2L)).getId(); var second = service.pay(secondId,1000);
        assertEquals(PAID, first.status()); assertEquals(PAID, second.status()); assertNotEquals(first.approvalId(),second.approvalId());
        assertEquals(2, gateway.approvals()); assertEquals(2, payments.count());
    }
}
