package io.github.kergosdyr.commercelab.domain.waitingroom;

import java.time.Duration;
import java.time.Instant;

public interface WaitingRoomRepository {

    EnqueuedTicket enqueue(
            String ticketId,
            String reservedAdmissionToken,
            Instant issuedAt,
            Instant expiresAt
    );

    PollDecision poll(
            String ticketId,
            Instant now,
            Instant admissionExpiresAt,
            Duration promotionReadinessTtl,
            int maxConcurrency
    );

    ClaimStatus claim(
            String ticketId,
            String admissionToken,
            Instant now,
            Instant processingLeaseExpiresAt
    );

    void release(String ticketId, String admissionToken, boolean completed);

    Metrics metrics(Instant now);

    void reset();

    record EnqueuedTicket(long sequence, long position) {
    }

    record PollDecision(
            PollStatus status,
            long position,
            String admissionToken,
            Instant admissionExpiresAt,
            long waitDurationMillis
    ) {

        public static PollDecision queued(long position) {
            return new PollDecision(PollStatus.QUEUED, position, null, null, 0);
        }

        public static PollDecision admitted(
                String admissionToken,
                Instant admissionExpiresAt,
                long waitDurationMillis
        ) {
            return new PollDecision(
                    PollStatus.ADMITTED,
                    0,
                    admissionToken,
                    admissionExpiresAt,
                    waitDurationMillis
            );
        }

        public static PollDecision expired() {
            return new PollDecision(PollStatus.EXPIRED, 0, null, null, 0);
        }
    }

    enum PollStatus {
        QUEUED,
        ADMITTED,
        EXPIRED
    }

    enum ClaimStatus {
        CLAIMED,
        INVALID,
        EXPIRED
    }

    record Metrics(
            long issued,
            long admitted,
            long completed,
            long downstreamRejected,
            long bypassRejected,
            long expiredTickets,
            long expiredAdmissions,
            long currentQueueDepth,
            long maxQueueDepth,
            long currentActive,
            long maxActive,
            long fifoViolations,
            long lastAdmittedSequence,
            long totalWaitDurationMillis,
            long maxWaitDurationMillis
    ) {
    }
}
