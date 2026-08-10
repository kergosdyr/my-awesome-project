package io.github.kergosdyr.commercelab.domain.cache;

import java.util.List;

public record CacheExperimentMetricsResult(
        CacheExperimentPolicy policy,
        List<StrategyMetrics> strategies
) {

    public CacheExperimentMetricsResult {
        strategies = List.copyOf(strategies);
    }

    public record StrategyMetrics(
            CacheStrategy strategy,
            long requests,
            long hits,
            long misses,
            long originLoads,
            long lockContention
    ) {
    }
}
