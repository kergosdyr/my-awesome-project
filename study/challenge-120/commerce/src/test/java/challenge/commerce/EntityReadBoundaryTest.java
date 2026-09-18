package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import challenge.commerce.infra.db.ProductEntity;
import challenge.commerce.infra.db.ProductOptionEntity;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** OSIV=false에서 HTTP 응답 변환까지 마친 뒤의 SQL 수를 검사한다. */
class EntityReadBoundaryTest extends CommerceHttpSupport {
    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void catalogQueryCountStaysFlatWhenMoreEntitiesAreReturned() throws Exception {
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean enabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        try {
            statistics.clear();
            var small = request("GET", "/api/products", "");
            assertEquals(200, small.code());
            assertEquals(3, small.body().size());
            assertEquals(2, statistics.getPrepareStatementCount());
            for (int id = 4; id <= 13; id++) {
                productJpaRepository.save(
                        new ProductEntity(id, "FORM", "Extra", "Description", "TOP", 1000, "/images/tee.svg"));
                productOptionJpaRepository.save(new ProductOptionEntity(id * 100L, id, "White", "M", 10));
            }
            statistics.clear();
            var large = request("GET", "/api/products", "");
            assertEquals(200, large.code());
            assertEquals(13, large.body().size());
            assertEquals(1, large.body().get(12).path("options").size());
            assertEquals(2, statistics.getPrepareStatementCount(), "상품·옵션 일괄 조회 이후 응답 변환에서 SQL 추가 없음");
        } finally {
            statistics.setStatisticsEnabled(enabled);
        }
    }

    @Test
    void ordersAndPaymentsRemainBoundedBatchReads() throws Exception {
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean enabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        try {
            assertEquals(200, pay(multiOrder()).code());
            statistics.clear();
            assertEquals(
                    1, request("GET", "/api/orders", "").body().path("entries").size());
            assertEquals(2, statistics.getPrepareStatementCount());
            multiOrder();
            multiOrder();
            statistics.clear();
            var result = request("GET", "/api/orders", "");
            assertEquals(200, result.code());
            assertEquals(3, result.body().path("entries").size());
            for (var detail : result.body().path("entries"))
                assertEquals(2, detail.path("order").path("items").size());
            assertEquals(2, statistics.getPrepareStatementCount(), "주문 수만큼 결제를 개별 조회하지 않는다");
        } finally {
            statistics.setStatisticsEnabled(enabled);
        }
    }

    private long multiOrder() throws Exception {
        var reply = request("POST", "/api/orders", """
                {"items":[{"optionId":101,"quantity":1,"displayedUnitPrice":129000},
                          {"optionId":201,"quantity":1,"displayedUnitPrice":39000}]}
                """);
        assertEquals(201, reply.code());
        return reply.body().path("id").asLong();
    }
}
