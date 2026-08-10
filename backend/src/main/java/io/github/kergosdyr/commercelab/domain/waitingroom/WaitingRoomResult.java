package io.github.kergosdyr.commercelab.domain.waitingroom;

import java.time.Instant;

public final class WaitingRoomResult {

    private WaitingRoomResult() {
    }

    public record Ticket(
            String ticketId,
            String status,
            long position,
            Instant issuedAt,
            Instant expiresAt,
            long pollAfterMillis
    ) {
    }

    public record Poll(
            String ticketId,
            String status,
            long position,
            String admissionToken,
            Instant admissionExpiresAt,
            long waitDurationMillis,
            long pollAfterMillis
    ) {

        public boolean queued() {
            return "QUEUED".equals(status);
        }
    }

    public record Purchase(
            String mode,
            String status,
            String ticketId,
            Instant processedAt,
            long processingDurationMillis
    ) {
    }

    public record Metrics(
            Policy policy,
            FlashSaleClient.Metrics downstream,
            WaitingRoomRepository.Metrics waitingRoom
    ) {
    }

    public record Policy(
            int maxConcurrency,
            long workDurationMillis,
            long ticketTtlMillis,
            long admissionTtlMillis,
            long processingLeaseTtlMillis,
            long promotionReadinessTtlMillis,
            long pollIntervalMillis
    ) {
    }
}
