package io.github.kergosdyr.commercelab.domain.eventlab;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.github.kergosdyr.commercelab.domain.order.PlacedOrderResult;

public record OrderPlacedEvent(
        UUID eventId,
        Long orderId,
        String orderNumber,
        BigDecimal totalAmount,
        Instant occurredAt
) {

    public static OrderPlacedEvent from(PlacedOrderResult placedOrder) {
        var order = placedOrder.order();
        return new OrderPlacedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }
}
