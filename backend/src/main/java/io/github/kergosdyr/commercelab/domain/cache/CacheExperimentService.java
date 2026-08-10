package io.github.kergosdyr.commercelab.domain.cache;

import io.github.kergosdyr.commercelab.domain.cache.CacheExperimentMetrics.RequestMetrics;
import io.github.kergosdyr.commercelab.domain.cache.CacheExperimentResult.Outcome;
import io.github.kergosdyr.commercelab.domain.product.ProductReader;
import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CacheExperimentService {

    private final ProductReader productReader;
    private final ProductCache productCache;
    private final CacheLock cacheLock;
    private final ExperimentDelay experimentDelay;
    private final CacheExperimentMetrics metrics;
    private final CacheExperimentPolicy policy;

    public CacheExperimentService(
            ProductReader productReader,
            ProductCache productCache,
            CacheLock cacheLock,
            ExperimentDelay experimentDelay,
            CacheExperimentMetrics metrics,
            CacheExperimentPolicy policy
    ) {
        this.productReader = productReader;
        this.productCache = productCache;
        this.cacheLock = cacheLock;
        this.experimentDelay = experimentDelay;
        this.metrics = metrics;
        this.policy = policy;
    }

    @Transactional(readOnly = true)
    public CacheExperimentResult readProduct(CacheProductQuery query) {
        var requestMetrics = metrics.begin(query.strategy());
        return switch (query.strategy()) {
            case NAIVE -> readNaively(query, requestMetrics);
            case PROTECTED -> readWithProtection(query, requestMetrics);
        };
    }

    @Transactional(readOnly = true)
    public CacheExperimentMetricsResult readMetrics() {
        return metrics.snapshot(policy);
    }

    @Transactional
    public CacheExperimentMetricsResult reset() {
        productCache.clearExperimentEntries();
        metrics.reset();
        return metrics.snapshot(policy);
    }

    private CacheExperimentResult readNaively(CacheProductQuery query, RequestMetrics requestMetrics) {
        var cachedProduct = productCache.read(query.strategy(), query.productId());
        if (cachedProduct.isPresent()) {
            requestMetrics.hit();
            return new CacheExperimentResult(query.strategy(), Outcome.CACHE_HIT, cachedProduct.get());
        }

        requestMetrics.miss();
        return loadOrigin(query, requestMetrics);
    }

    private CacheExperimentResult readWithProtection(CacheProductQuery query, RequestMetrics requestMetrics) {
        var cachedProduct = productCache.read(query.strategy(), query.productId());
        if (cachedProduct.isPresent()) {
            requestMetrics.hit();
            return new CacheExperimentResult(query.strategy(), Outcome.CACHE_HIT, cachedProduct.get());
        }

        requestMetrics.miss();
        var lockToken = cacheLock.tryAcquire(query.strategy(), query.productId(), policy.lockLease());
        if (lockToken.isPresent()) {
            return loadWhileHoldingLock(query, requestMetrics, lockToken.get());
        }

        requestMetrics.lockContention();
        for (var attempt = 0; attempt < policy.lockAttemptCount(); attempt++) {
            experimentDelay.pause(policy.lockPollInterval());

            cachedProduct = productCache.read(query.strategy(), query.productId());
            if (cachedProduct.isPresent()) {
                return new CacheExperimentResult(query.strategy(), Outcome.PEER_FILLED, cachedProduct.get());
            }

            lockToken = cacheLock.tryAcquire(query.strategy(), query.productId(), policy.lockLease());
            if (lockToken.isPresent()) {
                return loadWhileHoldingLock(query, requestMetrics, lockToken.get());
            }
        }

        throw new ApiException(ErrorType.CACHE_LOCK_TIMEOUT);
    }

    private CacheExperimentResult loadWhileHoldingLock(
            CacheProductQuery query,
            RequestMetrics requestMetrics,
            CacheLock.Token lockToken
    ) {
        try {
            var cachedProduct = productCache.read(query.strategy(), query.productId());
            if (cachedProduct.isPresent()) {
                return new CacheExperimentResult(query.strategy(), Outcome.PEER_FILLED, cachedProduct.get());
            }
            return loadOrigin(query, requestMetrics);
        } finally {
            cacheLock.release(lockToken);
        }
    }

    private CacheExperimentResult loadOrigin(CacheProductQuery query, RequestMetrics requestMetrics) {
        requestMetrics.originLoad();
        experimentDelay.pause(policy.originDelay());
        var product = CachedProduct.fromEntity(productReader.readProduct(query.productId()));
        productCache.write(query.strategy(), product, policy.ttl());
        return new CacheExperimentResult(query.strategy(), Outcome.ORIGIN_LOADED, product);
    }
}
