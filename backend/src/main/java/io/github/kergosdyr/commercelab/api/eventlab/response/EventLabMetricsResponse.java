package io.github.kergosdyr.commercelab.api.eventlab.response;

import io.github.kergosdyr.commercelab.domain.eventlab.EventLabMetricsResult;

public record EventLabMetricsResponse(
        long created,
        long published,
        long processed,
        long duplicates,
        long pending,
        Long oldestPendingAgeMs,
        long publishFailures
) {

    public static EventLabMetricsResponse from(EventLabMetricsResult result) {
        return new EventLabMetricsResponse(
                result.created(),
                result.published(),
                result.processed(),
                result.duplicates(),
                result.pending(),
                result.oldestPendingAgeMs(),
                result.publishFailures()
        );
    }
}
