package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** 주문 API의 공개 JSON에서 주문 공통 정보와 품목 목록을 분리한다. */
class OrderResponseContractTest extends CommerceHttpSupport {
    @Test
    void nanosecondClockPreservesCreatedTimeAfterDatabaseRoundTrip() throws Exception {
        var now = java.time.Instant.parse("2026-09-19T12:34:56.123456789Z");
        var item = challenge.commerce.infra.db.OrderItemEntity.select(
                productJpaRepository.findById(1L).orElseThrow(),
                productOptionJpaRepository.findById(101L).orElseThrow(),
                1);
        challenge.commerce.infra.db.OrderEntity order;
        try (var clock =
                org.mockito.Mockito.mockStatic(java.time.Instant.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            clock.when(java.time.Instant::now).thenReturn(now);
            order = challenge.commerce.infra.db.OrderEntity.place(java.util.List.of(item));
        }
        orderJpaRepository.saveAndFlush(order);
        var response = details(order.id());
        assertEquals(200, response.code());
        assertEquals(
                order.createdAt(),
                java.time.Instant.parse(
                        response.body().path("order").path("createdAt").asText()));
    }

    @Test
    void createdOrderAndUnpaidDetailsKeepTheirShape() throws Exception {
        var created = request(
                "POST", "/api/orders", "{\"items\":[{\"optionId\":101,\"quantity\":1,\"displayedUnitPrice\":129000}]}");
        assertEquals(201, created.code());
        var order = created.body();
        assertEquals(4, order.size());
        assertEquals(1, order.path("items").size());
        var item = order.path("items").get(0);
        assertTrue(order.path("id").asLong() > 0);
        assertEquals(101, item.path("optionId").asLong());
        assertEquals("유틸리티 필드 재킷", item.path("productName").asText());
        assertEquals("Olive / M", item.path("optionName").asText());
        assertEquals("/images/jacket.svg", item.path("image").asText());
        assertEquals(129000, item.path("unitPrice").asLong());
        assertEquals(1, item.path("quantity").asInt());
        assertEquals(129000, order.path("totalAmount").asLong());
        assertDoesNotThrow(() -> java.time.Instant.parse(order.path("createdAt").asText()));
        var detail = details(order.path("id").asLong());
        assertEquals(200, detail.code());
        assertEquals(3, detail.body().size());
        assertEquals(order, detail.body().path("order"));
        assertEquals("UNPAID", detail.body().path("paymentStatus").asText());
        assertTrue(detail.body().path("approvalId").isNull());
        assertEquals(
                detail.body(),
                request("GET", "/api/orders", "").body().path("entries").get(0));
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
        assertEquals(
                detail, request("GET", "/api/orders", "").body().path("entries").get(0));
    }
}
