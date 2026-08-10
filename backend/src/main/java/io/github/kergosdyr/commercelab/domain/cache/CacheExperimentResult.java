package io.github.kergosdyr.commercelab.domain.cache;

public record CacheExperimentResult(
        CacheStrategy strategy,
        Outcome outcome,
        CachedProduct product
) {

    public enum Outcome {
        CACHE_HIT,
        ORIGIN_LOADED,
        PEER_FILLED
    }
}
