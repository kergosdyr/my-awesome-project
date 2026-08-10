package io.github.kergosdyr.commercelab.infra.storage.mysql.eventlab;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_events_outbox")
class OutboxEventJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(name = "event_key", nullable = false, length = 100)
    private String eventKey;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "publish_attempts", nullable = false)
    private int publishAttempts;

    @Column(name = "publish_failures", nullable = false)
    private int publishFailures;

    @Column(name = "last_error", length = 500)
    private String lastError;

    protected OutboxEventJpaEntity() {
    }

    static OutboxEventJpaEntity pending(
            String eventId,
            Long aggregateId,
            String topic,
            String eventKey,
            String eventType,
            String payload,
            Instant occurredAt
    ) {
        var entity = new OutboxEventJpaEntity();
        entity.eventId = eventId;
        entity.aggregateId = aggregateId;
        entity.topic = topic;
        entity.eventKey = eventKey;
        entity.eventType = eventType;
        entity.payload = payload;
        entity.occurredAt = occurredAt;
        entity.publishAttempts = 0;
        entity.publishFailures = 0;
        return entity;
    }

    void markPublished(Instant publishedAt) {
        publishAttempts++;
        this.publishedAt = publishedAt;
        lastError = null;
    }

    void recordFailure(String message) {
        publishAttempts++;
        publishFailures++;
        if (message == null || message.isBlank()) {
            lastError = "Unknown Kafka publish failure";
            return;
        }
        lastError = message.substring(0, Math.min(message.length(), 500));
    }

    String getEventId() {
        return eventId;
    }

    String getPayload() {
        return payload;
    }

    String getTopic() {
        return topic;
    }

    String getEventKey() {
        return eventKey;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }
}
