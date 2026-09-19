package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import challenge.commerce.domain.catalog.ProductReader;
import challenge.commerce.infra.db.*;
import challenge.commerce.support.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class ProductEntityJoinTest extends CommerceHttpSupport {
    @Autowired
    ProductReader productReader;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void rootPageKeepsAllOptionsAndProductWithoutOptions() throws Exception {
        productJpaRepository.save(
                new ProductEntity(4, "FORM", "No options", "Description", "TOP", 1000, "/images/tee.svg"));
        var first = request("GET", "/api/products?size=1", "");
        assertEquals(200, first.code());
        assertEquals(1, first.body().size());
        assertEquals(3, first.body().get(0).path("options").size());
        assertEquals(101, first.body().get(0).path("options").get(0).path("id").asLong());
        var noOptions = request("GET", "/api/products?page=3&size=1", "");
        assertEquals(200, noOptions.code());
        assertEquals(4, noOptions.body().get(0).path("id").asLong());
        assertEquals(0, noOptions.body().get(0).path("options").size());
        assertEquals(0, request("GET", "/api/products?page=4&size=1", "").body().size());
    }

    @Test
    void selectedProductAndOptionAreManagedAndFetchedTogether() {
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean enabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                statistics.clear();
                var selected = productReader.readForOptions(List.of(101L, 102L, 201L));
                assertEquals(3, selected.size());
                assertEquals(1, statistics.getPrepareStatementCount());
                assertSame(selected.get(101L).product(), selected.get(102L).product());
                for (var result : selected.values()) {
                    assertTrue(entityManager.contains(result.product()));
                    assertTrue(entityManager.contains(result.option()));
                    assertEquals(result.product().id(), result.option().productId());
                }
            });
        } finally {
            statistics.setStatisticsEnabled(enabled);
        }
    }

    @Test
    void missingOptionOrMissingParentIsRejectedBeforeOrdering() {
        productOptionJpaRepository.save(new ProductOptionEntity(990, 999, "White", "M", 10));
        for (long id : new long[] {990, 991}) {
            var error = assertThrows(BusinessException.class, () -> productReader.readForOptions(List.of(id)));
            assertEquals(BusinessException.Reason.NOT_FOUND, error.reason());
        }
    }

    @Test
    void largeCatalogOnlyLoadsRequestedProductsAndValidatesBounds() throws Exception {
        productJpaRepository.saveAll(IntStream.range(4, 254)
                .mapToObj(id ->
                        new ProductEntity(id, "FORM", "Extra " + id, "Description", "TOP", 1000, "/images/tee.svg"))
                .toList());
        productOptionJpaRepository.saveAll(IntStream.range(4, 254)
                .mapToObj(id -> new ProductOptionEntity(1000 + id, id, "White", "M", 10))
                .toList());
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean enabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        try {
            statistics.clear();
            var result = request("GET", "/api/products?page=10&size=20", "");
            assertEquals(200, result.code());
            assertEquals(20, result.body().size());
            assertEquals(201, result.body().get(0).path("id").asLong());
            assertTrue(statistics.getEntityLoadCount() <= 40);
            assertEquals(2, statistics.getPrepareStatementCount());
            assertEquals(
                    100, request("GET", "/api/products?size=100", "").body().size());
            for (String query : List.of("?page=-1", "?size=0", "?size=101"))
                assertEquals(400, request("GET", "/api/products" + query, "").code());
        } finally {
            statistics.setStatisticsEnabled(enabled);
        }
    }
}
