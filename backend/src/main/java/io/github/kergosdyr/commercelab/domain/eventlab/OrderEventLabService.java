package io.github.kergosdyr.commercelab.domain.eventlab;

import java.time.Clock;

import io.github.kergosdyr.commercelab.domain.order.CreateOrderCommand;
import io.github.kergosdyr.commercelab.domain.order.OrderPlacer;
import io.github.kergosdyr.commercelab.support.monitoring.EventLabMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderEventLabService {

    private final OrderPlacer orderPlacer;
    private final OrderEventPublisher eventPublisher;
    private final OrderEventOutbox eventOutbox;
    private final ProcessedOrderEventRecorder processedEventRecorder;
    private final EventLabFixtureResetter fixtureResetter;
    private final EventLabMetrics metrics;
    private final Clock clock;

    public OrderEventLabService(
            OrderPlacer orderPlacer,
            OrderEventPublisher eventPublisher,
            OrderEventOutbox eventOutbox,
            ProcessedOrderEventRecorder processedEventRecorder,
            EventLabFixtureResetter fixtureResetter,
            EventLabMetrics metrics,
            Clock clock
    ) {
        this.orderPlacer = orderPlacer;
        this.eventPublisher = eventPublisher;
        this.eventOutbox = eventOutbox;
        this.processedEventRecorder = processedEventRecorder;
        this.fixtureResetter = fixtureResetter;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public LabOrderResult placeOrder(CreateOrderCommand command, EventPublicationStrategy strategy) {
        var order = orderPlacer.place(command);
        var event = OrderPlacedEvent.from(order);

        if (strategy == EventPublicationStrategy.DIRECT) {
            publishDirect(event);
        } else {
            eventOutbox.append(event);
        }

        metrics.recordCreatedAfterCommit();
        return new LabOrderResult(order, event, strategy);
    }

    @Transactional(readOnly = true)
    public EventLabMetricsResult readMetrics() {
        var counters = metrics.snapshot();
        var backlog = eventOutbox.readBacklog();
        return new EventLabMetricsResult(
                counters.created(),
                counters.published(),
                processedEventRecorder.countProcessed(),
                counters.duplicates(),
                backlog.pending(),
                backlog.oldestAgeMillis(clock.instant()),
                counters.publishFailures() + backlog.publishFailures()
        );
    }

    @Transactional
    public LabResetResult resetFixtures() {
        fixtureResetter.reset();
        metrics.resetAfterCommit();
        return new LabResetResult(true);
    }

    private void publishDirect(OrderPlacedEvent event) {
        try {
            eventPublisher.publish(event);
            metrics.recordPublishedAfterCommit();
        } catch (RuntimeException failure) {
            metrics.recordPublishFailureNow();
            throw failure;
        }
    }
}
