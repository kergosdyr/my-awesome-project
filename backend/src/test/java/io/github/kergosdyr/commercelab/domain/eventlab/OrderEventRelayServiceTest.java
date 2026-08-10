package io.github.kergosdyr.commercelab.domain.eventlab;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import io.github.kergosdyr.commercelab.support.monitoring.EventLabMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderEventRelayServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");

    @Mock
    private OrderEventOutbox eventOutbox;

    @Mock
    private OrderEventPublisher eventPublisher;

    private OrderEventRelayService relayService;

    @BeforeEach
    void setUp() {
        relayService = new OrderEventRelayService(
                eventOutbox,
                eventPublisher,
                new EventLabMetrics(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                10
        );
    }

    @Test
    void publishesSuccessfulRowsAndLeavesFailedRowsPending() {
        var published = event("published");
        var failed = event("failed");
        when(eventOutbox.readPendingForUpdate(10)).thenReturn(List.of(published, failed));
        org.mockito.Mockito.doAnswer(invocation -> {
            if (failed.equals(invocation.getArgument(0))) {
                throw new ApiException(ErrorType.EVENT_PUBLISH_FAILED);
            }
            return null;
        }).when(eventPublisher).publish(any(PendingOrderEvent.class));

        var attempted = relayService.relayPending();

        org.assertj.core.api.Assertions.assertThat(attempted).isEqualTo(2);
        verify(eventOutbox).markPublished(published, NOW);
        verify(eventOutbox).recordPublishFailure(
                org.mockito.ArgumentMatchers.eq(failed),
                org.mockito.ArgumentMatchers.any(ApiException.class)
        );
    }

    private PendingOrderEvent event(String suffix) {
        return new PendingOrderEvent(
                UUID.randomUUID(),
                "test-topic",
                "ORDER-" + suffix,
                "{\"event\":\"" + suffix + "\"}"
        );
    }
}
