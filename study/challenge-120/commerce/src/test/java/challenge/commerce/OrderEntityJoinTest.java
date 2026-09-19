package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import challenge.commerce.domain.order.OrderQueryService;
import challenge.commerce.infra.db.OrderEntity;
import challenge.commerce.infra.db.OrderPagingFixture;
import challenge.commerce.infra.db.PaymentEntity;
import jakarta.persistence.EntityManager;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class OrderEntityJoinTest extends CommerceHttpSupport {
    @Autowired
    DataSource dataSource;

    @Autowired
    EntityManager entityManager;

    @Autowired
    OrderQueryService orderQueryService;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void joinedEntitiesRemainManagedInCallingServiceTransaction() throws Exception {
        new OrderPagingFixture(dataSource).seed(3);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            // 호출 측 서비스의 쓰기 트랜잭션을 재현한다. 조회 서비스는 같은 트랜잭션에 참여한다.
            var result = orderQueryService.find(10_000);
            assertTrue(entityManager.contains(result.order()));
            assertTrue(entityManager.contains(result.payment()));
            assertSame(entityManager.find(OrderEntity.class, 10_000L), result.order());
            assertEquals(PaymentEntity.Status.PENDING, result.payment().status());
            assertEquals(2, result.order().items().size());
            result.payment().confirmApproval("joined-entity-approval");
            // save/merge 없이 트랜잭션 종료 시 변경 반영.
        });
        var detail = details(10_000);
        assertEquals(200, detail.code());
        assertEquals("PAID", detail.body().path("paymentStatus").asText());
        assertEquals("joined-entity-approval", detail.body().path("approvalId").asText());
    }

    @Test
    void missingPaymentKeepsOrderAndMissingOrderRemainsNotFound() throws Exception {
        new OrderPagingFixture(dataSource).seed(3);
        var detail = details(10_002);
        assertEquals(200, detail.code());
        assertEquals("UNPAID", detail.body().path("paymentStatus").asText());
        assertTrue(detail.body().path("approvalId").isNull());
        assertEquals(2, detail.body().path("order").path("items").size());
        assertEquals(404, details(999_999).code());
    }
}
