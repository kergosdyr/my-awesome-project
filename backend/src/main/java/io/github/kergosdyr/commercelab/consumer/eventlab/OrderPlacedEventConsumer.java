package io.github.kergosdyr.commercelab.consumer.eventlab;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kergosdyr.commercelab.domain.eventlab.OrderEventProcessingService;
import io.github.kergosdyr.commercelab.domain.eventlab.OrderPlacedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacedEventConsumer {

    private final ObjectMapper objectMapper;
    private final OrderEventProcessingService processingService;

    public OrderPlacedEventConsumer(
            ObjectMapper objectMapper,
            OrderEventProcessingService processingService
    ) {
        this.objectMapper = objectMapper;
        this.processingService = processingService;
    }

    @KafkaListener(
            topics = "${app.labs.kafka-outbox.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            autoStartup = "${app.labs.kafka-outbox.consumer-enabled:true}"
    )
    public void consume(String payload) {
        processingService.process(readEvent(payload));
    }

    private OrderPlacedEvent readEvent(String payload) {
        try {
            return objectMapper.readValue(payload, OrderPlacedEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Kafka order event payload is invalid", exception);
        }
    }
}
