package io.github.kergosdyr.commercelab.domain.eventlab;

import java.time.Clock;

import io.github.kergosdyr.commercelab.support.monitoring.EventLabMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderEventProcessingService {

    private final ProcessedOrderEventRecorder processedEventRecorder;
    private final EventLabMetrics metrics;
    private final Clock clock;

    public OrderEventProcessingService(
            ProcessedOrderEventRecorder processedEventRecorder,
            EventLabMetrics metrics,
            Clock clock
    ) {
        this.processedEventRecorder = processedEventRecorder;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public void process(OrderPlacedEvent event) {
        var firstDelivery = processedEventRecorder.recordIfFirst(event, clock.instant());
        if (!firstDelivery) {
            metrics.recordDuplicateAfterCommit();
        }
    }
}
