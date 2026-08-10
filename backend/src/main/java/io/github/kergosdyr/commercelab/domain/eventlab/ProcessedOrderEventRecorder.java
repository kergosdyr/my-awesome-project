package io.github.kergosdyr.commercelab.domain.eventlab;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProcessedOrderEventRecorder {

    private final ProcessedOrderEventRepository processedEventRepository;

    public ProcessedOrderEventRecorder(ProcessedOrderEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public boolean recordIfFirst(OrderPlacedEvent event, Instant processedAt) {
        return processedEventRepository.recordIfFirst(event, processedAt);
    }

    @Transactional(readOnly = true)
    public long countProcessed() {
        return processedEventRepository.countProcessed();
    }
}
