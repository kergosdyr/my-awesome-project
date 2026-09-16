package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** 제공 환경의 회귀 검사. Day6 알림 정답의 성공 여부와 분리한다. */
class CommerceInfrastructureTest extends CommerceHttpSupport {
    @Test
    void catalogContainsRealOptionsIncludingSoldOut() throws Exception {
        var response = request("GET", "/api/products", "");
        assertEquals(200, response.code());
        assertEquals(3, response.body().size());
        var jacket = response.body().get(0);
        assertEquals(129000, jacket.get("price").asLong());
        assertEquals(3, jacket.get("options").size());
        assertEquals(0, jacket.get("options").get(2).get("stock").asInt());
    }

    @Test
    void orderUsesServerPriceAndConsumesOptionStock() throws Exception {
        var response = request(
                "POST", "/api/orders", "{\"displayedUnitPrice\":129000,\"optionId\":101,\"quantity\":2,\"amount\":1}");
        assertEquals(201, response.code());
        assertEquals(258000, response.body().get("totalAmount").asLong());
        assertEquals("유틸리티 필드 재킷", response.body().get("productName").asText());
        assertEquals(
                10,
                request("GET", "/api/products", "")
                        .body()
                        .get(0)
                        .get("options")
                        .get(0)
                        .get("stock")
                        .asInt());
        assertEquals(
                "UNPAID",
                details(response.body().get("id").asLong())
                        .body()
                        .get("paymentStatus")
                        .asText());
    }

    @Test
    void soldOutAndInvalidQuantitiesDoNotCreateOrders() throws Exception {
        assertEquals(
                409,
                request("POST", "/api/orders", "{\"displayedUnitPrice\":129000,\"optionId\":103,\"quantity\":1}")
                        .code());
        assertEquals(
                400,
                request("POST", "/api/orders", "{\"displayedUnitPrice\":129000,\"optionId\":101,\"quantity\":0}")
                        .code());
        assertEquals(
                400,
                request("POST", "/api/orders", "{\"displayedUnitPrice\":129000,\"optionId\":101,\"quantity\":6}")
                        .code());
        assertEquals(
                404,
                request("POST", "/api/orders", "{\"displayedUnitPrice\":129000,\"optionId\":999,\"quantity\":1}")
                        .code());
        assertEquals(0, orders.count());
        assertEquals(
                12,
                request("GET", "/api/products", "")
                        .body()
                        .get(0)
                        .get("options")
                        .get(0)
                        .get("stock")
                        .asInt());
    }

    @Test
    void repeatedOrdersCannotOversellStock() throws Exception {
        assertEquals(
                201,
                request("POST", "/api/orders", "{\"displayedUnitPrice\":129000,\"optionId\":102,\"quantity\":5}")
                        .code());
        assertEquals(
                409,
                request("POST", "/api/orders", "{\"displayedUnitPrice\":129000,\"optionId\":102,\"quantity\":5}")
                        .code());
        assertEquals(1, orders.count());
        assertEquals(
                3,
                request("GET", "/api/products", "")
                        .body()
                        .get(0)
                        .get("options")
                        .get(1)
                        .get("stock")
                        .asInt());
    }

    @Test
    void synchronousPaymentAndRepeatKeepOneApprovalAndOneRow() throws Exception {
        long id = order();
        var paid = pay(id);
        assertEquals(200, paid.code());
        assertEquals("PAID", paid.body().get("status").asText());
        assertEquals(paid.body(), pay(id).body());
        assertEquals(1, payments.count());
        assertEquals(1, gateway.approvals());
        assertEquals(129000, gateway.lookup(String.valueOf(id)).orElseThrow().amount());
        assertEquals("PAID", details(id).body().get("paymentStatus").asText());
        assertEquals(1, request("GET", "/api/orders", "").body().size());
    }

    @Test
    void lostResponseUsesExistingGatewayReceipt() throws Exception {
        request("PUT", "/dev/pg/mode/APPROVE_THEN_LOSE_RESPONSE", "");
        var paid = pay(order());
        assertEquals(200, paid.code());
        assertEquals("PAID", paid.body().get("status").asText());
        assertEquals(1, gateway.approvals());
        assertEquals(1, payments.count());
    }

    @Test
    void gatewayApprovalAloneDoesNotChangeLocalPayment() throws Exception {
        long id = order();
        pendingThenComplete(id);
        assertEquals("PENDING", details(id).body().get("paymentStatus").asText());
        assertEquals(1, payments.count());
    }

    @Test
    void missingOrderCannotRequestAnApproval() throws Exception {
        assertEquals(404, pay(99999).code());
        assertEquals(0, gateway.approvals());
        assertEquals(0, payments.count());
    }

    @Test
    void storefrontAndDeveloperAssetsAreServedBySameApplication() throws Exception {
        for (var path : new String[] {
            "/",
            "/store.css",
            "/store.js",
            "/shared.js",
            "/images/jacket.svg",
            "/images/tee.svg",
            "/images/tote.svg",
            "/dev.html",
            "/dev.js"
        }) {
            var response = raw("GET", path, "");
            assertEquals(200, response.statusCode(), path);
            assertFalse(response.body().isBlank(), path);
        }
        assertTrue(raw("GET", "/", "").body().contains("Everyday,"));
    }
}
