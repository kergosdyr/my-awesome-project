package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Day6 사용자 구현 계약. 현재 onNotification의 insert 동작 때문에 실패하는 것이 과제 출발점이다. */
class PaymentNotificationContractTest extends CommerceHttpSupport {
    @Test
    void approvedNotificationUpdatesTheExistingPayment() throws Exception {
        long id = order();
        var receipt = pendingThenComplete(id);
        var rowId = paymentStore.findByOrderId(id).orElseThrow().id();
        var response = notifyApproval(receipt);
        assertEquals(200, response.code());
        assertTrue(response.body().get("accepted").asBoolean());
        assertEquals(1, payments.count());
        var after = paymentStore.findByOrderId(id).orElseThrow();
        assertEquals(rowId, after.id());
        assertTrue(after.isPaid());
        assertEquals(receipt.get("approvalId").asText(), after.approvalId());
        assertEquals("PAID", details(id).body().get("paymentStatus").asText());
    }

    @Test
    void repeatedNotificationAndPayKeepTheSameApproval() throws Exception {
        long id = order();
        var receipt = pendingThenComplete(id);
        assertEquals(200, notifyApproval(receipt).code());
        assertEquals(200, notifyApproval(receipt).code());
        assertEquals(1, payments.count());
        assertEquals(receipt.get("approvalId").asText(), pay(id).body().get("approvalId").asText());
        assertEquals(1, gateway.approvals());
    }

    @Test
    void notificationWithoutLocalPaymentIsIgnored() throws Exception {
        long id = order();
        var receipt = json.valueToTree(gateway.approve(String.valueOf(id), id, 129000));
        var response = notifyApproval(receipt);
        assertEquals(200, response.code());
        assertFalse(response.body().get("accepted").asBoolean());
        assertEquals(0, payments.count());
        assertEquals("UNPAID", details(id).body().get("paymentStatus").asText());
    }

    @Test
    @Tag("full")
    void notificationAfterSynchronousPaymentPreservesIdentity() throws Exception {
        long id = order();
        var paid = pay(id);
        var before = paymentStore.findByOrderId(id).orElseThrow();
        var receipt = json.valueToTree(gateway.lookup(String.valueOf(id)).orElseThrow());
        assertEquals(200, notifyApproval(receipt).code());
        assertEquals(before.id(), paymentStore.findByOrderId(id).orElseThrow().id());
        assertEquals(paid.body(), pay(id).body());
        assertEquals(1, payments.count());
    }
}
