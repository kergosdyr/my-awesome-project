package io.github.kergosdyr.commercelab.domain.eventlab;

import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EventLabFixtureResetter {

    private final EventLabFixtureRepository fixtureRepository;
    private final boolean resetEnabled;

    public EventLabFixtureResetter(
            EventLabFixtureRepository fixtureRepository,
            @Value("${app.labs.kafka-outbox.reset-enabled:false}") boolean resetEnabled
    ) {
        this.fixtureRepository = fixtureRepository;
        this.resetEnabled = resetEnabled;
    }

    @Transactional
    public void reset() {
        if (!resetEnabled) {
            throw new ApiException(ErrorType.LAB_RESET_DISABLED);
        }
        fixtureRepository.reset();
    }
}
