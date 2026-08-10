package io.github.kergosdyr.commercelab.infra.client.kafka.eventlab;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kergosdyr.commercelab.domain.eventlab.OrderEventPublisher;
import io.github.kergosdyr.commercelab.domain.eventlab.OrderPlacedEvent;
import io.github.kergosdyr.commercelab.domain.eventlab.PendingOrderEvent;
import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventKafkaPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final Duration publishTimeout;

    public OrderEventKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.labs.kafka-outbox.topic}") String topic,
            @Value("${app.labs.kafka-outbox.publish-timeout:5s}") Duration publishTimeout
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.publishTimeout = publishTimeout;
    }

    @Override
    public void publish(OrderPlacedEvent event) {
        try {
            var payload = objectMapper.writeValueAsString(event);
            send(topic, event.orderNumber(), payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Order event could not be serialized", exception);
        }
    }

    @Override
    public void publish(PendingOrderEvent event) {
        send(event.topic(), event.eventKey(), event.payload());
    }

    private void send(String destination, String eventKey, String payload) {
        try {
            kafkaTemplate.send(destination, eventKey, payload)
                    .get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException(ErrorType.EVENT_PUBLISH_FAILED);
        }
    }
}
