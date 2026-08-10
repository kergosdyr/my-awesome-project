package io.github.kergosdyr.commercelab.domain.eventlab;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderEventOutboxRepository {

    void append(OrderPlacedEvent event);

    List<PendingOrderEvent> readPendingForUpdate(int batchSize);

    void markPublished(UUID eventId, Instant publishedAt);

    void recordPublishFailure(UUID eventId, String message);

    OutboxBacklog readBacklog();
}
