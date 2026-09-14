package challenge.lab;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** 이미 통과한 결제와 새 HTTP/PG 실행 장치. 알림 업무 정답과 분리한다. */
class PaymentHttpInfrastructureTest extends PaymentHttpSupport {
    @Test
    void normalPaymentRoundTripAndRepeat() throws Exception {
        var first = pay(1);
        assertEquals(200, first.code());
        assertEquals("PAID", first.body().get("status").asText());
        assertFalse(first.body().get("approvalId").asText().isBlank());
        assertEquals(first.body(), pay(1).body());
        assertEquals(first.body(), request("GET", "/payments/1", "").body());
        assertEquals(1, gateway.approvals());
        assertEquals(1, payments.count());
    }

    @Test
    void invalidRequestStopsAtHttpBoundary() throws Exception {
        assertEquals(
                400, request("POST", "/payments", "{\"reservationId\":1,\"amount\":0}").code());
        assertEquals(400, request("POST", "/payments", "{}").code());
        assertEquals(0, payments.count());
        assertEquals(0, gateway.approvals());
    }

    @Test
    void missingPaymentAndReservationReturn404() throws Exception {
        assertEquals(404, request("GET", "/payments/1", "").code());
        assertEquals(404, pay(999).code());
        assertEquals(0, payments.count());
        assertEquals(0, gateway.approvals());
    }

    @Test
    void providerCompletionDoesNotMutateLocalDatabase() throws Exception {
        var approved = pendingThenComplete();
        assertEquals("APPROVED", approved.get("status").asText());
        assertEquals(approved, request("POST", "/lab/pg/1/complete", "").body());
        var local = request("GET", "/payments/1", "");
        assertEquals(202, local.code());
        assertEquals("PENDING", local.body().get("status").asText());
        assertTrue(local.body().get("approvalId").isNull());
        assertEquals(1, gateway.approvals());
    }

    @Test
    void responseLossKeepsExistingDay5Behavior() throws Exception {
        request("PUT", "/lab/pg/mode/APPROVE_THEN_LOSE_RESPONSE", "");
        var first = pay(1);
        assertEquals(200, first.code());
        assertEquals(first.body(), pay(1).body());
        assertEquals(1, gateway.approvals());
        assertEquals(1, payments.count());
    }
}
