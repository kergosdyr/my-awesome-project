package io.github.kergosdyr.commercelab.api.waitingroom.response;

import java.time.Instant;

import io.github.kergosdyr.commercelab.domain.waitingroom.FlashSaleClient;
import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomRepository;
import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomResult;

public final class WaitingRoomResponse {

    private WaitingRoomResponse() {
    }

    public record Ticket(
            String ticketId,
            String status,
            long position,
            Instant issuedAt,
            Instant expiresAt,
            long pollAfterMillis
    ) {

        public static Ticket fromResult(WaitingRoomResult.Ticket result) {
            return new Ticket(
                    result.ticketId(),
                    result.status(),
                    result.position(),
                    result.issuedAt(),
                    result.expiresAt(),
                    result.pollAfterMillis()
            );
        }
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

        public static Poll fromResult(WaitingRoomResult.Poll result) {
            return new Poll(
                    result.ticketId(),
                    result.status(),
                    result.position(),
                    result.admissionToken(),
                    result.admissionExpiresAt(),
                    result.waitDurationMillis(),
                    result.pollAfterMillis()
            );
        }
    }

    public record Purchase(
            String mode,
            String status,
            String ticketId,
            Instant processedAt,
            long processingDurationMillis
    ) {

        public static Purchase fromResult(WaitingRoomResult.Purchase result) {
            return new Purchase(
                    result.mode(),
                    result.status(),
                    result.ticketId(),
                    result.processedAt(),
                    result.processingDurationMillis()
            );
        }
    }

    public record Metrics(
            WaitingRoomResult.Policy policy,
            FlashSaleClient.Metrics downstream,
            WaitingRoomRepository.Metrics waitingRoom
    ) {

        public static Metrics fromResult(WaitingRoomResult.Metrics result) {
            return new Metrics(result.policy(), result.downstream(), result.waitingRoom());
        }
    }
}
