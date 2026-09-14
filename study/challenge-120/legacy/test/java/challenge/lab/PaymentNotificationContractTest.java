package challenge.lab;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Day6 새 계약. 실제 socket HTTP -> Controller -> Service -> DB 커밋을 확인한다. */
class PaymentNotificationContractTest extends PaymentHttpSupport {
    @Test
    void approvalNotificationCompletesSamePayment() throws Exception {
        var approved = pendingThenComplete();
        var id = payments.findByReservationId(1L).orElseThrow().getId();
        var response = notifyApproval(approved);
        assertEquals(200, response.code());
        assertTrue(response.body().get("accepted").asBoolean());
        var local = request("GET", "/payments/1", "");
        assertEquals(200, local.code());
        assertEquals("PAID", local.body().get("status").asText());
        assertEquals(approved.get("approvalId"), local.body().get("approvalId"));
        assertEquals(id, payments.findByReservationId(1L).orElseThrow().getId());
        assertEquals(1, payments.count());
        assertEquals(1, gateway.approvals());
    }

    @Test
    void duplicateNotificationAndLaterPayKeepSameApproval() throws Exception {
        var approved = pendingThenComplete();
        assertEquals(200, notifyApproval(approved).code());
        var repeated = notifyApproval(approved);
        assertEquals(200, repeated.code());
        assertTrue(repeated.body().get("accepted").asBoolean());
        var result = pay(1);
        assertEquals(200, result.code());
        assertEquals(approved.get("approvalId"), result.body().get("approvalId"));
        assertEquals(1, payments.count());
        assertEquals(1, gateway.approvals());
    }

    @Test
    void unknownPaymentNotificationDoesNotCreatePayment() throws Exception {
        var response =
                request(
                        "POST",
                        "/payments/notifications",
                        "{\"key\":\"2\",\"reservationId\":2,\"amount\":1000,\"status\":\"APPROVED\",\"approvalId\":\"external-2\"}");
        assertEquals(200, response.code());
        assertFalse(response.body().get("accepted").asBoolean());
        assertEquals(0, payments.count());
        assertEquals(0, gateway.approvals());
        assertEquals(404, request("GET", "/payments/2", "").code());
    }

    @Test
    @Tag("full")
    void notificationAfterSynchronousApprovalPreservesIdentity() throws Exception {
        var paid = pay(1);
        var id = payments.findByReservationId(1L).orElseThrow().getId();
        var receipt = request("GET", "/lab/pg/1", "").body();
        assertEquals(200, notifyApproval(receipt).code());
        assertEquals(paid.body(), request("GET", "/payments/1", "").body());
        assertEquals(id, payments.findByReservationId(1L).orElseThrow().getId());
        assertEquals(1, payments.count());
        assertEquals(1, gateway.approvals());
    }
}
