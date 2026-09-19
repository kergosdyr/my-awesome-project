package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import challenge.commerce.infra.db.OrderPagingFixture;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/** 기본 제공 OFFSET 페이지 조회 회귀 검사. B009의 커서 구현과 무관하다. */
class OrderPageTest extends CommerceHttpSupport {
    @Autowired
    DataSource dataSource;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @AfterEach
    void clearFixture() {
        new OrderPagingFixture(dataSource).clear();
    }

    private JsonNode page(String query) throws Exception {
        var result = request("GET", "/api/orders" + query, "");
        assertEquals(200, result.code());
        return result.body();
    }

    private List<Long> ids(JsonNode result) {
        var ids = new ArrayList<Long>();
        for (var entry : result.path("entries"))
            ids.add(entry.path("order").path("id").asLong());
        return ids;
    }

    @Test
    void emptyAndOutOfRangeAreEmptyPages() throws Exception {
        var empty = page("");
        assertEquals(List.of(), ids(empty));
        assertFalse(empty.path("hasNext").asBoolean());
        assertEquals(0, empty.path("page").asInt());
        assertEquals(20, empty.path("size").asInt());
        new OrderPagingFixture(dataSource).seed(3);
        var beyond = page("?page=2&size=3");
        assertEquals(List.of(), ids(beyond));
        assertFalse(beyond.path("hasNext").asBoolean());
    }

    @Test
    void defaultAndMaximumSizesAreBounded() throws Exception {
        var expected = new OrderPagingFixture(dataSource)
                .seed(125).stream().map(OrderPagingFixture.Row::id).toList();
        var first = page("");
        assertEquals(expected.subList(0, 20), ids(first));
        assertTrue(first.path("hasNext").asBoolean());
        var max = page("?size=100");
        assertEquals(expected.subList(0, 100), ids(max));
        assertTrue(max.path("hasNext").asBoolean());
        var last = page("?page=1&size=100");
        assertEquals(expected.subList(100, 125), ids(last));
        assertFalse(last.path("hasNext").asBoolean());
    }

    @Test
    void walkPagesKeepsTiesItemsAndPayments() throws Exception {
        var expected = new OrderPagingFixture(dataSource)
                .seed(47).stream().map(OrderPagingFixture.Row::id).toList();
        var seen = new ArrayList<Long>();
        for (int i = 0; i < 7; i++) {
            var result = page("?page=" + i + "&size=7");
            seen.addAll(ids(result));
            assertEquals(i, result.path("page").asInt());
            assertEquals(7, result.path("size").asInt());
            assertEquals(i < 6, result.path("hasNext").asBoolean());
            for (var entry : result.path("entries")) {
                var order = entry.path("order");
                long id = order.path("id").asLong();
                assertEquals(2, order.path("items").size());
                assertEquals(3000, order.path("totalAmount").asLong());
                assertEquals(
                        id % 3 == 0 ? "UNPAID" : id % 3 == 1 ? "PENDING" : "PAID",
                        entry.path("paymentStatus").asText());
                if (id % 3 == 2)
                    assertEquals("approval-" + id, entry.path("approvalId").asText());
            }
        }
        assertEquals(expected, seen);
    }

    @Test
    void exactLastPageHasNoNextAndRepeatIsStable() throws Exception {
        new OrderPagingFixture(dataSource).seed(6);
        var last = page("?page=1&size=3");
        assertEquals(3, ids(last).size());
        assertFalse(last.path("hasNext").asBoolean());
        assertEquals(last, page("?page=1&size=3"));
    }

    @Test
    void invalidPageSizeAndOffsetAreRejected() throws Exception {
        for (String query :
                List.of("?page=-1", "?size=0", "?size=101", "?size=-2", "?page=2147483647&size=100", "?page=nope"))
            assertEquals(400, request("GET", "/api/orders" + query, "").code(), query);
    }

    @Test
    void largeDatabaseDoesNotLoadUnrequestedEntities() throws Exception {
        new OrderPagingFixture(dataSource).seed(20_000);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean enabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        try {
            for (int index : new int[] {0, 900}) {
                statistics.clear();
                var result = page("?page=" + index + "&size=20");
                assertEquals(20, ids(result).size());
                assertTrue(statistics.getEntityLoadCount() <= 80, "20개 주문·40개 품목·최대20개 결제만 엔티티로 읽어야 한다");
                assertTrue(statistics.getPrepareStatementCount() <= 4, "주문 수에 비례한 개별 조회 방지");
            }
        } finally {
            statistics.setStatisticsEnabled(enabled);
        }
    }
}
