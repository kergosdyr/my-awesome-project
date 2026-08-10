package io.github.kergosdyr.commercelab.api.eventlab.response;

import java.util.UUID;

import io.github.kergosdyr.commercelab.domain.eventlab.EventPublicationStrategy;
import io.github.kergosdyr.commercelab.domain.eventlab.LabOrderResult;

public record LabOrderResponse(
        Long orderId,
        String orderNumber,
        UUID eventId,
        EventPublicationStrategy strategy
) {

    public static LabOrderResponse from(LabOrderResult result) {
        var order = result.order().order();
        return new LabOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                result.event().eventId(),
                result.strategy()
        );
    }
}
