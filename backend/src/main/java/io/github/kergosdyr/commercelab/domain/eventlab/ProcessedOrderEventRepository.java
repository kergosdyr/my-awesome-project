package io.github.kergosdyr.commercelab.domain.eventlab;

import java.time.Instant;

public interface ProcessedOrderEventRepository {

    boolean recordIfFirst(OrderPlacedEvent event, Instant processedAt);

    long countProcessed();
}
