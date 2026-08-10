package io.github.kergosdyr.commercelab.domain.cache;

import java.time.Duration;
import java.util.Optional;

public interface CacheLock {

    Optional<Token> tryAcquire(CacheStrategy strategy, Long productId, Duration lease);

    void release(Token token);

    record Token(
            String key,
            String value
    ) {
    }
}
