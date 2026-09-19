package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import challenge.commerce.infra.db.OrderPagingFixture;
import java.time.Instant;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 새 TODO에 의존하지 않고 제공 데이터가 실제 기존 API로 읽히는지 확인한다. */
class OrderPagingFixtureTest extends CommerceHttpSupport {
    @Autowired
    DataSource source;

    @Test
    void suppliedRowsAreReadableThroughExistingApi() throws Exception {
        var expected = new OrderPagingFixture(source)
                .seed(11).stream().collect(Collectors.toMap(OrderPagingFixture.Row::id, Function.identity()));
        var response = request("GET", "/api/orders", "");
        assertEquals(200, response.code());
        assertEquals(11, response.body().path("entries").size());
        for (var entry : response.body().path("entries")) {
            var order = entry.path("order");
            long id = order.path("id").asLong();
            assertEquals(
                    expected.get(id).time(),
                    Instant.parse(order.path("createdAt").asText()));
            assertEquals(2, order.path("items").size());
            assertEquals(3000, order.path("totalAmount").asLong());
            assertEquals(1000, order.path("items").get(0).path("unitPrice").asLong());
            assertEquals(2000, order.path("items").get(1).path("unitPrice").asLong());
            assertEquals(
                    id % 3 == 0 ? "UNPAID" : id % 3 == 1 ? "PENDING" : "PAID",
                    entry.path("paymentStatus").asText());
            if (id % 3 == 2)
                assertEquals("approval-" + id, entry.path("approvalId").asText());
            else assertTrue(entry.path("approvalId").isNull());
        }
    }
}
