package io.github.kergosdyr.commercelab.domain.eventlab;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderEventOutbox {

    private final OrderEventOutboxRepository outboxRepository;

    public OrderEventOutbox(OrderEventOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public void append(OrderPlacedEvent event) {
        outboxRepository.append(event);
    }

    @Transactional
    public List<PendingOrderEvent> readPendingForUpdate(int batchSize) {
        return outboxRepository.readPendingForUpdate(batchSize);
    }

    @Transactional
    public void markPublished(PendingOrderEvent event, Instant publishedAt) {
        outboxRepository.markPublished(event.eventId(), publishedAt);
    }

    @Transactional
    public void recordPublishFailure(PendingOrderEvent event, RuntimeException failure) {
        outboxRepository.recordPublishFailure(event.eventId(), failure.getMessage());
    }

    @Transactional(readOnly = true)
    public OutboxBacklog readBacklog() {
        return outboxRepository.readBacklog();
    }
}
