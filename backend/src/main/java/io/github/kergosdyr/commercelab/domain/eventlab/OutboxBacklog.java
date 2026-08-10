package io.github.kergosdyr.commercelab.domain.eventlab;

import java.time.Duration;
import java.time.Instant;

public record OutboxBacklog(
        long pending,
        Instant oldestOccurredAt,
        long publishFailures
) {

    public Long oldestAgeMillis(Instant now) {
        if (oldestOccurredAt == null) {
            return null;
        }
        return Math.max(0, Duration.between(oldestOccurredAt, now).toMillis());
    }
}
