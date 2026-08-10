package io.github.kergosdyr.commercelab.domain.eventlab;

import java.util.UUID;

public record PendingOrderEvent(
        UUID eventId,
        String topic,
        String eventKey,
        String payload
) {
}
