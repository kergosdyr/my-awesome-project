package io.github.kergosdyr.commercelab.api.cache.response;

import java.util.List;

import io.github.kergosdyr.commercelab.domain.cache.CacheExperimentMetricsResult;
import io.github.kergosdyr.commercelab.domain.cache.CacheStrategy;

public record CacheExperimentMetricsResponse(
        long cacheTtlMs,
        long originDelayMs,
        long lockLeaseMs,
        long lockWaitTimeoutMs,
        long lockPollIntervalMs,
        List<StrategyMetrics> strategies
) {

    public CacheExperimentMetricsResponse {
        strategies = List.copyOf(strategies);
    }

    public static CacheExperimentMetricsResponse fromResult(CacheExperimentMetricsResult result) {
        var policy = result.policy();
        var strategies = result.strategies().stream()
                .map(StrategyMetrics::fromResult)
                .toList();
        return new CacheExperimentMetricsResponse(
                policy.ttl().toMillis(),
                policy.originDelay().toMillis(),
                policy.lockLease().toMillis(),
                policy.lockWaitTimeout().toMillis(),
                policy.lockPollInterval().toMillis(),
                strategies
        );
    }

    public record StrategyMetrics(
            CacheStrategy strategy,
            long requests,
            long hits,
            long misses,
            long originLoads,
            long lockContention
    ) {

        private static StrategyMetrics fromResult(CacheExperimentMetricsResult.StrategyMetrics metrics) {
            return new StrategyMetrics(
                    metrics.strategy(),
                    metrics.requests(),
                    metrics.hits(),
                    metrics.misses(),
                    metrics.originLoads(),
                    metrics.lockContention()
            );
        }
    }
}
