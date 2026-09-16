package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** B007 공개 완료 기준. 호출 횟수·내부 순서·클래스 배치를 강제하지 않는다. */
class PriceConsentTest extends PriceExperimentSupport {
    @Test
    void saleEndedBeforeOrder() throws Exception {
        price(39000);
        rejectedWithoutChanges(buy(29000, 2));
    }

    @Test
    void evenOneWonIncreaseNeedsConsent() throws Exception {
        price(29001);
        rejectedWithoutChanges(buy(29000, 1));
    }

    @Test
    void samePriceCreatesOrderAtServerPrice() throws Exception {
        price(29000);
        var reply = buy(29000, 2);
        assertEquals(201, reply.code());
        assertEquals(58000, reply.body().path("totalAmount").asLong());
        assertEquals(29000, reply.body().path("unitPrice").asLong());
        assertEquals(1, orders.count());
        assertEquals(18, stock());
    }

    @Test
    void lowerPriceAllowsEitherDocumentedPolicy() throws Exception {
        price(29000);
        var reply = buy(39000, 2);
        if (reply.code() == 409) {
            rejectedWithoutChanges(reply);
        } else {
            assertEquals(201, reply.code());
            assertEquals(58000, reply.body().path("totalAmount").asLong(), "새 서버 가격을 적용한다");
            assertEquals(1, orders.count());
            assertEquals(18, stock());
        }
    }

    @Test
    void afterReadingNewPriceCustomerCanOrderAgain() throws Exception {
        price(39000);
        rejectedWithoutChanges(buy(29000, 1));
        long newPrice =
                request("GET", "/api/products", "").body().get(1).path("price").asLong();
        var reply = buy(newPrice, 1);
        assertEquals(201, reply.code());
        assertEquals(39000, reply.body().path("totalAmount").asLong());
        assertEquals(1, orders.count());
        assertEquals(19, stock());
    }

    @Test
    void clientCannotChooseOneWonPrice() throws Exception {
        price(39000);
        rejectedWithoutChanges(buy(1, 1));
    }

    @Test
    void existingOrderAndApprovalKeepAgreedAmount() throws Exception {
        price(29000);
        var reply = buy(29000, 2);
        assertEquals(201, reply.code());
        long id = reply.body().path("id").asLong();
        price(39000);
        assertEquals(200, pay(id).code());
        assertEquals(58000, details(id).body().path("order").path("totalAmount").asLong());
        assertEquals(58000, gateway.lookup(String.valueOf(id)).orElseThrow().amount());
        assertEquals(18, stock());
    }

    @Test
    void missingDisplayedPriceIsBadRequest() throws Exception {
        assertEquals(
                400,
                request("POST", "/api/orders", "{\"optionId\":201,\"quantity\":1}")
                        .code());
        assertEquals(0, orders.count());
        assertEquals(20, stock());
    }
}
