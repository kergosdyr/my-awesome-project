package io.github.kergosdyr.commercelab.domain.eventlab;

public record EventLabMetricsResult(
        long created,
        long published,
        long processed,
        long duplicates,
        long pending,
        Long oldestPendingAgeMs,
        long publishFailures
) {
}
