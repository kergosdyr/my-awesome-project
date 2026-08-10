package io.github.kergosdyr.commercelab.domain.eventlab;

public interface OrderEventPublisher {

    void publish(OrderPlacedEvent event);

    void publish(PendingOrderEvent event);
}
