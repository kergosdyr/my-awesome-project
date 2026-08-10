package io.github.kergosdyr.commercelab.scheduler.eventlab;

import io.github.kergosdyr.commercelab.domain.eventlab.OrderEventRelayService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.labs.kafka-outbox.relay-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OrderEventOutboxRelay {

    private final OrderEventRelayService relayService;

    public OrderEventOutboxRelay(OrderEventRelayService relayService) {
        this.relayService = relayService;
    }

    @Scheduled(fixedDelayString = "${app.labs.kafka-outbox.relay-delay-ms:500}")
    public void relay() {
        relayService.relayPending();
    }
}
