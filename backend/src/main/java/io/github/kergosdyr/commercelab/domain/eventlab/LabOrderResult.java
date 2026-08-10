package io.github.kergosdyr.commercelab.domain.eventlab;

import io.github.kergosdyr.commercelab.domain.order.PlacedOrderResult;

public record LabOrderResult(
        PlacedOrderResult order,
        OrderPlacedEvent event,
        EventPublicationStrategy strategy
) {
}
