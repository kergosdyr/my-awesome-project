package io.github.kergosdyr.commercelab.infra.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableKafka
@EnableScheduling
public class KafkaOutboxLabConfig {

    @Bean
    @ConditionalOnProperty(
            name = "app.labs.kafka-outbox.topic-auto-create",
            havingValue = "true",
            matchIfMissing = true
    )
    NewTopic orderPlacedLabTopic(
            @Value("${app.labs.kafka-outbox.topic}") String topic,
            @Value("${app.labs.kafka-outbox.partitions:3}") int partitions
    ) {
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(1)
                .build();
    }
}
