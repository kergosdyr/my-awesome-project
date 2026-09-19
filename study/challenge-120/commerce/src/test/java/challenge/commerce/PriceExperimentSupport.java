package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** AI 제공 재현 장치. 업무 서비스에 장애 주입 코드나 수동 트랜잭션을 넣지 않는다. */
abstract class PriceExperimentSupport extends CommerceHttpSupport {
    @Autowired
    JdbcTemplate database;

    void price(long amount) {
        assertEquals(1, database.update("update store_product set price = ? where id = 2", amount));
    }

    int stock() throws Exception {
        return request("GET", "/api/products", "")
                .body()
                .get(1)
                .get("options")
                .get(0)
                .get("stock")
                .asInt();
    }

    Reply buy(long displayedPrice, int quantity) throws Exception {
        return request("POST", "/api/orders", """
                {"items":[{"optionId":201,"quantity":%d,"displayedUnitPrice":%d}]}
                """.formatted(quantity, displayedPrice));
    }

    void rejectedWithoutChanges(Reply reply) throws Exception {
        assertAll(
                () -> assertEquals(409, reply.code(), "가격을 다시 확인할 수 있어야 한다"),
                () -> assertEquals(0, orderJpaRepository.count(), "거절한 주문을 남기지 않는다"),
                () -> assertEquals(20, stock(), "거절한 주문 때문에 재고가 줄지 않는다"),
                () -> assertEquals(0, paymentJpaRepository.count()),
                () -> assertEquals(0, fakePaymentGateway.approvals()));
        assertFalse(reply.body().path("message").asText().isBlank(), "사용자에게 이유를 안내한다");
    }
}
