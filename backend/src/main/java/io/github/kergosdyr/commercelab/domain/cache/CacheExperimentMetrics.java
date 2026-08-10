package io.github.kergosdyr.commercelab.domain.cache;

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.stereotype.Component;

@Component
public class CacheExperimentMetrics {

    private final AtomicReference<EnumMap<CacheStrategy, Counters>> state =
            new AtomicReference<>(newCounters());

    public RequestMetrics begin(CacheStrategy strategy) {
        var counters = state.get().get(strategy);
        counters.requests.increment();
        return new RequestMetrics(counters);
    }

    public CacheExperimentMetricsResult snapshot(CacheExperimentPolicy policy) {
        var current = state.get();
        var strategies = current.entrySet().stream()
                .map(entry -> entry.getValue().snapshot(entry.getKey()))
                .toList();
        return new CacheExperimentMetricsResult(policy, strategies);
    }

    public void reset() {
        state.set(newCounters());
    }

    private static EnumMap<CacheStrategy, Counters> newCounters() {
        var counters = new EnumMap<CacheStrategy, Counters>(CacheStrategy.class);
        for (var strategy : CacheStrategy.values()) {
            counters.put(strategy, new Counters());
        }
        return counters;
    }

    public static final class RequestMetrics {

        private final Counters counters;

        private RequestMetrics(Counters counters) {
            this.counters = counters;
        }

        public void hit() {
            counters.hits.increment();
        }

        public void miss() {
            counters.misses.increment();
        }

        public void originLoad() {
            counters.originLoads.increment();
        }

        public void lockContention() {
            counters.lockContention.increment();
        }
    }

    private static final class Counters {

        private final LongAdder requests = new LongAdder();
        private final LongAdder hits = new LongAdder();
        private final LongAdder misses = new LongAdder();
        private final LongAdder originLoads = new LongAdder();
        private final LongAdder lockContention = new LongAdder();

        private CacheExperimentMetricsResult.StrategyMetrics snapshot(CacheStrategy strategy) {
            return new CacheExperimentMetricsResult.StrategyMetrics(
                    strategy,
                    requests.sum(),
                    hits.sum(),
                    misses.sum(),
                    originLoads.sum(),
                    lockContention.sum()
            );
        }
    }
}
