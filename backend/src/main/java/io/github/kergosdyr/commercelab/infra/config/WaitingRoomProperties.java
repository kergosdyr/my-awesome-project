package io.github.kergosdyr.commercelab.infra.config;

import java.time.Duration;

import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomPolicy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.labs.waiting-room")
public record WaitingRoomProperties(
        @Min(1) int maxConcurrency,
        @NotNull Duration workDuration,
        @NotNull Duration ticketTtl,
        @NotNull Duration admissionTtl,
        @NotNull Duration processingLeaseTtl,
        @DefaultValue("1s") @NotNull Duration pollInterval
) implements WaitingRoomPolicy {

    public WaitingRoomProperties {
        requirePositive(workDuration, "work-duration");
        requirePositive(ticketTtl, "ticket-ttl");
        requirePositive(admissionTtl, "admission-ttl");
        requirePositive(processingLeaseTtl, "processing-lease-ttl");
        requirePositive(pollInterval, "poll-interval");
        if (processingLeaseTtl.compareTo(workDuration) <= 0) {
            throw new IllegalArgumentException("processing-lease-ttl must exceed work-duration");
        }
    }

    private static void requirePositive(Duration duration, String propertyName) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must be positive");
        }
    }
}
