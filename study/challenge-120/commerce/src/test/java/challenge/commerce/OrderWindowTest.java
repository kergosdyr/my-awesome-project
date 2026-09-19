package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import challenge.commerce.domain.order.OrderCursor;
import challenge.commerce.infra.db.OrderPagingFixture;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/** 결과·연속 조회·HTTP 직렬화를 검증한다. SQL 횟수나 내부 알고리즘은 강제하지 않는다. */
class OrderWindowTest extends CommerceHttpSupport {
    @Autowired
    DataSource source;

    private OrderPagingFixture data() {
        return new OrderPagingFixture(source);
    }

    private JsonNode page(int size, OrderCursor after) throws Exception {
        var response = request(
                "GET",
                "/api/orders/window?size=" + size
                        + (after == null
                                ? ""
                                : "&afterCreatedAt="
                                        + URLEncoder.encode(after.createdAt().toString(), StandardCharsets.UTF_8)
                                        + "&afterId=" + after.id()),
                "");
        assertEquals(200, response.code(), "B009 페이지 응답: " + response.body());
        assertTrue(response.body().path("entries").isArray());
        assertTrue(response.body().has("next"));
        return response.body();
    }

    private OrderCursor next(JsonNode page) {
        var next = page.path("next");
        if (next.isNull()) return null;
        assertTrue(next.isObject());
        assertTrue(next.path("createdAt").isTextual());
        assertTrue(next.path("id").isIntegralNumber());
        return new OrderCursor(
                Instant.parse(next.path("createdAt").asText()), next.path("id").asLong());
    }

    private List<Long> ids(JsonNode page) {
        var result = new ArrayList<Long>();
        for (var entry : page.path("entries"))
            result.add(entry.path("order").path("id").asLong());
        return result;
    }

    @Test
    void empty() throws Exception {
        var page = page(3, null);
        assertEquals(List.of(), ids(page));
        assertNull(next(page));
    }

    @Test
    void firstPageAndStableRepeat() throws Exception {
        var expected = data().seed(12).stream().map(OrderPagingFixture.Row::id).toList();
        var first = page(3, null);
        assertEquals(expected.subList(0, 3), ids(first));
        assertNotNull(next(first));
        assertEquals(ids(first), ids(page(3, null)));
    }

    @Test
    void allItemsAndPaymentStatesSurviveSerialization() throws Exception {
        data().seed(6);
        var result = page(10, null);
        assertEquals(6, ids(result).size());
        assertNull(next(result));
        for (var entry : result.path("entries")) {
            var order = entry.path("order");
            long id = order.path("id").asLong();
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

    @Test
    @Tag("full")
    void newestInsertBetweenRequestsDoesNotRepeatOrSkip() throws Exception {
        var expected = data().seed(12).stream().map(OrderPagingFixture.Row::id).toList();
        var first = page(3, null);
        var seen = new ArrayList<>(ids(first));
        var token = next(first);
        assertNotNull(token);
        data().appendLatest();
        for (int guard = 0; token != null && guard < 10; guard++) {
            var current = page(3, token);
            var retry = page(3, token);
            assertEquals(ids(current), ids(retry));
            assertEquals(next(current), next(retry));
            assertTrue(ids(current).size() <= 3);
            seen.addAll(ids(current));
            token = next(current);
        }
        assertNull(token, "끝없이 같은 표식을 반환하면 안 된다");
        assertEquals(expected, seen);
        assertEquals(900000L, ids(page(3, null)).getFirst());
    }

    @Test
    @Tag("full")
    void tiedTimesAcrossSizeOnePagesAndShortLastPage() throws Exception {
        var expected = data().seed(11).stream().map(OrderPagingFixture.Row::id).toList();
        for (int size : new int[] {1, 4}) {
            var seen = new ArrayList<Long>();
            OrderCursor token = null;
            for (int guard = 0; guard < 15; guard++) {
                var current = page(size, token);
                assertTrue(ids(current).size() <= size);
                seen.addAll(ids(current));
                token = next(current);
                if (token == null) break;
            }
            assertNull(token);
            assertEquals(expected, seen);
        }
    }

    @Test
    @Tag("full")
    void exactFinalPageHasNoNext() throws Exception {
        data().seed(6);
        var first = page(3, null);
        assertNotNull(next(first));
        var last = page(3, next(first));
        assertEquals(3, ids(last).size());
        assertNull(next(last));
    }

    @Test
    @Tag("full")
    void suppliedSizeValidation() throws Exception {
        for (int size : new int[] {0, 101, -1})
            assertEquals(
                    400, request("GET", "/api/orders/window?size=" + size, "").code());
    }
}
