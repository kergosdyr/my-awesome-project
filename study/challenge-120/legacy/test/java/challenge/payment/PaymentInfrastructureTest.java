package challenge.payment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class PaymentInfrastructureTest extends PaymentTestSupport {
    @Autowired FakePaymentGateway gateway;
    @Autowired ReservationRepository reservations;
    @Autowired PaymentRepository payments;
    @Autowired PlatformTransactionManager transactions;

    @BeforeEach
    void clear() {
        payments.deleteAll();
        reservations.deleteAll();
        gateway.reset();
    }

    @Test
    void responseLossIsIdempotentAndRejectsChangedPayload() {
        gateway.mode(FakePaymentGateway.Mode.APPROVE_THEN_LOSE_RESPONSE);
        assertThrows(
                PaymentGateway.ResponseLostException.class, () -> gateway.approve("k", 1, 1000));
        var receipt = gateway.approve("k", 1, 1000);
        assertEquals(PaymentGateway.Status.APPROVED, receipt.status());
        assertEquals(1, gateway.approvals());
        assertEquals(receipt, gateway.lookup("k").orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> gateway.approve("k", 1, 2000));
        assertTrue(gateway.lookup("missing").isEmpty());
    }

    @Test
    void fakeApprovalSurvivesLocalRollback() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new TransactionTemplate(transactions)
                                .execute(
                                        status -> {
                                            reservations.save(new Reservation(99L));
                                            gateway.approve("external", 99, 1000);
                                            throw new IllegalStateException("local failure");
                                        }));
        assertFalse(reservations.existsById(99L));
        assertEquals(1, gateway.approvals());
    }

    @Test
    void pendingProviderCompletesOnlyOnce() {
        gateway.mode(FakePaymentGateway.Mode.PROCESSING);
        var receipt = gateway.approve("p", 1, 1000);
        assertEquals(PaymentGateway.Status.PROCESSING, receipt.status());
        assertEquals(0, gateway.approvals());
        var approved = gateway.complete("p");
        assertEquals(approved, gateway.complete("p"));
        assertEquals(1, gateway.approvals());
    }

    @Test
    void injectedAbortRollsBackLocalWriteButKeepsExternalApproval() {
        gateway.mode(FakePaymentGateway.Mode.APPROVE_THEN_ABORT);
        assertThrows(
                FakePaymentGateway.SimulatedProcessStop.class,
                () ->
                        new TransactionTemplate(transactions)
                                .execute(
                                        status -> {
                                            reservations.save(new Reservation(77L));
                                            return gateway.approve("abort", 77, 1000);
                                        }));
        assertFalse(reservations.existsById(77L));
        assertEquals(1, gateway.approvals());
        assertEquals(PaymentGateway.Status.APPROVED, gateway.onlyReceipt().status());
        gateway.mode(FakePaymentGateway.Mode.NORMAL);
        assertEquals(gateway.onlyReceipt(), gateway.approve("abort", 77, 1000));
        assertEquals(1, gateway.approvals());
    }

    @Test
    void independentReservationFixtureIsAvailable() {
        reservations.save(new Reservation(1L));
        assertTrue(reservations.existsById(1L));
        assertFalse(reservations.existsById(999L));
        assertEquals(1, reservations.count());
        assertEquals(0, payments.count());
        assertEquals(0, gateway.approveCalls());
    }
}
