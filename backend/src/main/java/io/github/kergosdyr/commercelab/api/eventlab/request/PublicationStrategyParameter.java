package io.github.kergosdyr.commercelab.api.eventlab.request;

import java.util.Locale;

import io.github.kergosdyr.commercelab.domain.eventlab.EventPublicationStrategy;
import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;

public final class PublicationStrategyParameter {

    private PublicationStrategyParameter() {
    }

    public static EventPublicationStrategy toDomain(String value) {
        try {
            return EventPublicationStrategy.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ApiException(ErrorType.VALIDATION_ERROR, "strategy는 direct 또는 outbox여야 합니다.");
        }
    }
}
