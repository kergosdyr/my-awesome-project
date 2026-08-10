package io.github.kergosdyr.commercelab.domain.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import io.github.kergosdyr.commercelab.domain.cache.CacheExperimentResult.Outcome;
import io.github.kergosdyr.commercelab.domain.product.ProductEntity;
import io.github.kergosdyr.commercelab.domain.product.ProductReader;
import io.github.kergosdyr.commercelab.domain.product.ProductStatus;
import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CacheExperimentServiceTest {

    @Mock
    private ProductReader productReader;

    @Mock
    private ProductCache productCache;

    @Mock
    private CacheLock cacheLock;

    @Mock
    private ExperimentDelay experimentDelay;

    private CacheExperimentMetrics metrics;
    private CacheExperimentPolicy policy;
    private CacheExperimentService service;

    @BeforeEach
    void setUp() {
        metrics = new CacheExperimentMetrics();
        policy = new CacheExperimentPolicy(
                Duration.ofMillis(500),
                Duration.ZERO,
                Duration.ofSeconds(2),
                Duration.ofMillis(20),
                Duration.ofMillis(10)
        );
        service = new CacheExperimentService(
                productReader,
                productCache,
                cacheLock,
                experimentDelay,
                metrics,
                policy
        );
    }

    @Test
    void naiveCacheAsideLoadsTheOriginOnMissAndHitsOnTheNextRequest() {
        var cachedProduct = productSnapshot();
        var originProduct = productEntity();
        when(productCache.read(CacheStrategy.NAIVE, 1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(cachedProduct));
        when(productReader.readProduct(1L)).thenReturn(originProduct);

        var first = service.readProduct(new CacheProductQuery(1L, CacheStrategy.NAIVE));
        var second = service.readProduct(new CacheProductQuery(1L, CacheStrategy.NAIVE));

        assertThat(first.outcome()).isEqualTo(Outcome.ORIGIN_LOADED);
        assertThat(second.outcome()).isEqualTo(Outcome.CACHE_HIT);
        verify(productReader).readProduct(1L);
        verify(productCache).write(CacheStrategy.NAIVE, cachedProduct, policy.ttl());
        assertMetrics(CacheStrategy.NAIVE, 2, 1, 1, 1, 0);
    }

    @Test
    void protectedCacheAsideDoubleChecksAndReleasesTheOwnedLock() {
        var token = new CacheLock.Token("lock-key", "owner-token");
        var originProduct = productEntity();
        when(productCache.read(CacheStrategy.PROTECTED, 1L)).thenReturn(Optional.empty());
        when(cacheLock.tryAcquire(CacheStrategy.PROTECTED, 1L, policy.lockLease()))
                .thenReturn(Optional.of(token));
        when(productReader.readProduct(1L)).thenReturn(originProduct);

        var result = service.readProduct(new CacheProductQuery(1L, CacheStrategy.PROTECTED));

        assertThat(result.outcome()).isEqualTo(Outcome.ORIGIN_LOADED);
        verify(productCache, times(2)).read(CacheStrategy.PROTECTED, 1L);
        verify(cacheLock).release(token);
        assertMetrics(CacheStrategy.PROTECTED, 1, 0, 1, 1, 0);
    }

    @Test
    void protectedWaiterUsesTheValueFilledByTheLockOwner() {
        var cachedProduct = productSnapshot();
        when(productCache.read(CacheStrategy.PROTECTED, 1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(cachedProduct));
        when(cacheLock.tryAcquire(CacheStrategy.PROTECTED, 1L, policy.lockLease()))
                .thenReturn(Optional.empty());

        var result = service.readProduct(new CacheProductQuery(1L, CacheStrategy.PROTECTED));

        assertThat(result.outcome()).isEqualTo(Outcome.PEER_FILLED);
        verify(productReader, never()).readProduct(anyLong());
        verify(experimentDelay).pause(policy.lockPollInterval());
        assertMetrics(CacheStrategy.PROTECTED, 1, 0, 1, 0, 1);
    }

    @Test
    void protectedStrategyFailsClosedAfterTheBoundedLockWait() {
        when(productCache.read(CacheStrategy.PROTECTED, 1L)).thenReturn(Optional.empty());
        when(cacheLock.tryAcquire(CacheStrategy.PROTECTED, 1L, policy.lockLease()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.readProduct(new CacheProductQuery(1L, CacheStrategy.PROTECTED)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorType()).isEqualTo(ErrorType.CACHE_LOCK_TIMEOUT)
                );

        verify(cacheLock, times(3)).tryAcquire(CacheStrategy.PROTECTED, 1L, policy.lockLease());
        verify(experimentDelay, times(2)).pause(policy.lockPollInterval());
        verify(productReader, never()).readProduct(anyLong());
        assertMetrics(CacheStrategy.PROTECTED, 1, 0, 1, 0, 1);
    }

    @Test
    void resetClearsOnlyThroughTheExperimentCachePortAndZerosMetrics() {
        when(productCache.read(CacheStrategy.NAIVE, 1L)).thenReturn(Optional.of(productSnapshot()));
        service.readProduct(new CacheProductQuery(1L, CacheStrategy.NAIVE));

        var resetMetrics = service.reset();

        verify(productCache).clearExperimentEntries();
        var naive = resetMetrics.strategies().stream()
                .filter(strategy -> strategy.strategy() == CacheStrategy.NAIVE)
                .findFirst()
                .orElseThrow();
        assertThat(naive.requests()).isZero();
        assertThat(naive.hits()).isZero();
    }

    private void assertMetrics(
            CacheStrategy strategy,
            long requests,
            long hits,
            long misses,
            long originLoads,
            long lockContention
    ) {
        var strategyMetrics = service.readMetrics().strategies().stream()
                .filter(metrics -> metrics.strategy() == strategy)
                .findFirst()
                .orElseThrow();
        assertThat(strategyMetrics.requests()).isEqualTo(requests);
        assertThat(strategyMetrics.hits()).isEqualTo(hits);
        assertThat(strategyMetrics.misses()).isEqualTo(misses);
        assertThat(strategyMetrics.originLoads()).isEqualTo(originLoads);
        assertThat(strategyMetrics.lockContention()).isEqualTo(lockContention);
    }

    private ProductEntity productEntity() {
        var product = mock(ProductEntity.class);
        when(product.getId()).thenReturn(1L);
        when(product.getSku()).thenReturn("BEAN-HAEUNDAE-1KG");
        when(product.getName()).thenReturn("해운대 블렌드 원두 1kg");
        when(product.getDescription()).thenReturn("테스트 설명");
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(28_900));
        when(product.getStockQuantity()).thenReturn(120);
        when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);
        return product;
    }

    private CachedProduct productSnapshot() {
        return new CachedProduct(
                1L,
                "BEAN-HAEUNDAE-1KG",
                "해운대 블렌드 원두 1kg",
                "테스트 설명",
                BigDecimal.valueOf(28_900),
                120,
                ProductStatus.ACTIVE
        );
    }
}
