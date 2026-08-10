package io.github.kergosdyr.commercelab.infra.storage.mysql.eventlab;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kergosdyr.commercelab.domain.eventlab.OrderEventOutboxRepository;
import io.github.kergosdyr.commercelab.domain.eventlab.OrderPlacedEvent;
import io.github.kergosdyr.commercelab.domain.eventlab.OutboxBacklog;
import io.github.kergosdyr.commercelab.domain.eventlab.PendingOrderEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class OrderEventOutboxRepositoryImpl implements OrderEventOutboxRepository {

    private static final String EVENT_TYPE = "OrderPlaced";

    private final OutboxEventJpaRepository outboxJpaRepository;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OrderEventOutboxRepositoryImpl(
            OutboxEventJpaRepository outboxJpaRepository,
            ObjectMapper objectMapper,
            @Value("${app.labs.kafka-outbox.topic}") String topic
    ) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void append(OrderPlacedEvent event) {
        var entity = OutboxEventJpaEntity.pending(
                event.eventId().toString(),
                event.orderId(),
                topic,
                event.orderNumber(),
                EVENT_TYPE,
                serialize(event),
                event.occurredAt()
        );
        outboxJpaRepository.save(entity);
    }

    @Override
    public List<PendingOrderEvent> readPendingForUpdate(int batchSize) {
        var entities = outboxJpaRepository.findPendingForUpdate(PageRequest.of(0, batchSize));
        var events = new ArrayList<PendingOrderEvent>(entities.size());
        for (var entity : entities) {
            events.add(new PendingOrderEvent(
                    UUID.fromString(entity.getEventId()),
                    entity.getTopic(),
                    entity.getEventKey(),
                    entity.getPayload()
            ));
        }
        return List.copyOf(events);
    }

    @Override
    public void markPublished(UUID eventId, java.time.Instant publishedAt) {
        readEvent(eventId).markPublished(publishedAt);
    }

    @Override
    public void recordPublishFailure(UUID eventId, String message) {
        readEvent(eventId).recordFailure(message);
    }

    @Override
    public OutboxBacklog readBacklog() {
        var oldest = outboxJpaRepository.findFirstByPublishedAtIsNullOrderByOccurredAtAscEventIdAsc()
                .map(OutboxEventJpaEntity::getOccurredAt)
                .orElse(null);
        return new OutboxBacklog(
                outboxJpaRepository.countByPublishedAtIsNull(),
                oldest,
                outboxJpaRepository.sumPublishFailures()
        );
    }

    private OutboxEventJpaEntity readEvent(UUID eventId) {
        return outboxJpaRepository.findById(eventId.toString())
                .orElseThrow(() -> new IllegalStateException("Outbox event disappeared: " + eventId));
    }

    private String serialize(OrderPlacedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Order event could not be serialized", exception);
        }
    }

}
