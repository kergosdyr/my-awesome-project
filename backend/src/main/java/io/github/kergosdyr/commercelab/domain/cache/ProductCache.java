package io.github.kergosdyr.commercelab.domain.cache;

import java.time.Duration;
import java.util.Optional;

public interface ProductCache {

    Optional<CachedProduct> read(CacheStrategy strategy, Long productId);

    void write(CacheStrategy strategy, CachedProduct product, Duration ttl);

    void clearExperimentEntries();
}
