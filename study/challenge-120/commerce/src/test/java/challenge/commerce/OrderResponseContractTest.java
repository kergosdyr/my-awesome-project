package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** 주문 API의 공개 JSON 형태를 내부 모델 분리 전후 동일하게 유지한다. */
class OrderResponseContractTest extends CommerceHttpSupport {
    @Test
    void createdOrderAndUnpaidDetailsKeepTheirShape() throws Exception {
        var created = request("POST", "/api/orders", "{\"optionId\":101,\"quantity\":1,\"displayedUnitPrice\":129000}");
        assertEquals(201, created.code());
        var order = created.body();
        assertEquals(9, order.size());
        assertTrue(order.path("id").asLong() > 0);
        assertEquals(101, order.path("optionId").asLong());
        assertEquals("유틸리티 필드 재킷", order.path("productName").asText());
        assertEquals("Olive / M", order.path("optionName").asText());
        assertEquals("/images/jacket.svg", order.path("image").asText());
        assertEquals(129000, order.path("unitPrice").asLong());
        assertEquals(1, order.path("quantity").asInt());
        assertEquals(129000, order.path("totalAmount").asLong());
        assertDoesNotThrow(() -> java.time.Instant.parse(order.path("createdAt").asText()));
        var detail = details(order.path("id").asLong());
        assertEquals(200, detail.code());
        assertEquals(3, detail.body().size());
        assertEquals(order, detail.body().path("order"));
        assertEquals("UNPAID", detail.body().path("paymentStatus").asText());
        assertTrue(detail.body().path("approvalId").isNull());
        assertEquals(detail.body(), request("GET", "/api/orders", "").body().get(0));
    }

    @Test
    void pendingAndPaidDetailsKeepStatusAndApproval() throws Exception {
        long id = order();
        var receipt = pendingThenComplete(id);
        assertEquals("PENDING", details(id).body().path("paymentStatus").asText());
        assertTrue(details(id).body().path("approvalId").isNull());
        assertEquals(200, notifyApproval(receipt).code());
        var detail = details(id).body();
        assertEquals(3, detail.size());
        assertEquals("PAID", detail.path("paymentStatus").asText());
        assertEquals(receipt.path("approvalId"), detail.path("approvalId"));
        assertEquals(detail, request("GET", "/api/orders", "").body().get(0));
    }
}
