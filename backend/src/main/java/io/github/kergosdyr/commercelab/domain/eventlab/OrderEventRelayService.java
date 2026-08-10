package io.github.kergosdyr.commercelab.domain.eventlab;

import java.time.Clock;

import io.github.kergosdyr.commercelab.support.monitoring.EventLabMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderEventRelayService {

    private final OrderEventOutbox eventOutbox;
    private final OrderEventPublisher eventPublisher;
    private final EventLabMetrics metrics;
    private final Clock clock;
    private final int batchSize;

    public OrderEventRelayService(
            OrderEventOutbox eventOutbox,
            OrderEventPublisher eventPublisher,
            EventLabMetrics metrics,
            Clock clock,
            @Value("${app.labs.kafka-outbox.relay-batch-size:50}") int batchSize
    ) {
        this.eventOutbox = eventOutbox;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Transactional
    public int relayPending() {
        var events = eventOutbox.readPendingForUpdate(batchSize);
        for (var event : events) {
            try {
                eventPublisher.publish(event);
                eventOutbox.markPublished(event, clock.instant());
                metrics.recordPublishedAfterCommit();
            } catch (RuntimeException failure) {
                eventOutbox.recordPublishFailure(event, failure);
            }
        }
        return events.size();
    }
}
