package io.github.kergosdyr.commercelab.domain.waitingroom;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WaitingRoomTestFixtures {

    private WaitingRoomTestFixtures() {
    }

    public record TestPolicy(
            int maxConcurrency,
            Duration workDuration,
            Duration ticketTtl,
            Duration admissionTtl,
            Duration processingLeaseTtl,
            Duration pollInterval
    ) implements WaitingRoomPolicy {

        public static TestPolicy oneSlot() {
            return new TestPolicy(
                    1,
                    Duration.ofMillis(1),
                    Duration.ofSeconds(30),
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(1)
            );
        }
    }

    public static final class MutableClock extends Clock {

        private Instant instant;

        public MutableClock(Instant instant) {
            this.instant = instant;
        }

        public void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    public static final class FakeFlashSaleClient implements FlashSaleClient {

        private boolean accepting = true;
        private long accepted;
        private long rejected;

        public void rejectRequests() {
            accepting = false;
        }

        @Override
        public Attempt tryPurchase() {
            if (!accepting) {
                rejected++;
                return new Attempt(false, 0);
            }
            accepted++;
            return new Attempt(true, 1);
        }

        @Override
        public Metrics metrics() {
            return new Metrics(accepted, rejected, 0, accepted > 0 ? 1 : 0);
        }

        @Override
        public void reset() {
            accepted = 0;
            rejected = 0;
        }
    }

    public static final class FakeWaitingRoomRepository implements WaitingRoomRepository {

        private final Map<String, TicketState> tickets = new LinkedHashMap<>();
        private final Map<String, ActiveState> active = new HashMap<>();
        private long nextSequence;
        private long issued;
        private long admitted;
        private long completed;
        private long downstreamRejected;
        private long bypassRejected;
        private long expiredTickets;
        private long expiredAdmissions;
        private long maxQueueDepth;
        private long maxActive;
        private long fifoViolations;
        private long lastAdmittedSequence;
        private long totalWaitDurationMillis;
        private long maxWaitDurationMillis;

        @Override
        public synchronized EnqueuedTicket enqueue(
                String ticketId,
                String reservedAdmissionToken,
                Instant issuedAt,
                Instant expiresAt
        ) {
            cleanup(issuedAt);
            var sequence = ++nextSequence;
            tickets.put(
                    ticketId,
                    new TicketState(sequence, reservedAdmissionToken, issuedAt, expiresAt)
            );
            issued++;
            var depth = queueDepth();
            maxQueueDepth = Math.max(maxQueueDepth, depth);
            return new EnqueuedTicket(sequence, depth);
        }

        @Override
        public synchronized PollDecision poll(
                String ticketId,
                Instant now,
                Instant admissionExpiresAt,
                int maxConcurrency
        ) {
            cleanup(now);
            var ticket = tickets.get(ticketId);
            if (ticket == null) {
                return PollDecision.expired();
            }
            if (ticket.admissionToken != null) {
                var activeState = active.get(ticket.admissionToken);
                return activeState == null
                        ? PollDecision.expired()
                        : PollDecision.admitted(
                                ticket.admissionToken,
                                activeState.expiresAt,
                                Duration.between(ticket.issuedAt, ticket.admittedAt).toMillis()
                        );
            }

            var position = queuedTickets().entrySet().stream()
                    .map(Map.Entry::getKey)
                    .toList()
                    .indexOf(ticketId) + 1L;
            if (position <= 0) {
                return PollDecision.expired();
            }
            if (position == 1 && active.size() < maxConcurrency) {
                admitRequestedTicket(ticketId, ticket, now, admissionExpiresAt);
                return PollDecision.admitted(
                        ticket.admissionToken,
                        admissionExpiresAt,
                        Duration.between(ticket.issuedAt, ticket.admittedAt).toMillis()
                );
            }
            return PollDecision.queued(position);
        }

        @Override
        public synchronized ClaimStatus claim(
                String ticketId,
                String admissionToken,
                Instant now,
                Instant processingLeaseExpiresAt
        ) {
            cleanup(now);
            var ticket = tickets.get(ticketId);
            if (ticket == null) {
                return ClaimStatus.EXPIRED;
            }
            var activeState = active.get(admissionToken);
            if (!admissionToken.equals(ticket.admissionToken)
                    || activeState == null
                    || !ticketId.equals(activeState.ticketId)
                    || ticket.claimed) {
                bypassRejected++;
                return ClaimStatus.INVALID;
            }
            ticket.claimed = true;
            active.put(admissionToken, new ActiveState(ticketId, processingLeaseExpiresAt));
            return ClaimStatus.CLAIMED;
        }

        @Override
        public synchronized void release(String ticketId, String admissionToken, boolean wasCompleted) {
            active.remove(admissionToken);
            tickets.remove(ticketId);
            if (wasCompleted) {
                completed++;
            } else {
                downstreamRejected++;
            }
        }

        @Override
        public synchronized Metrics metrics(Instant now) {
            cleanup(now);
            return new Metrics(
                    issued,
                    admitted,
                    completed,
                    downstreamRejected,
                    bypassRejected,
                    expiredTickets,
                    expiredAdmissions,
                    queueDepth(),
                    maxQueueDepth,
                    active.size(),
                    maxActive,
                    fifoViolations,
                    lastAdmittedSequence,
                    totalWaitDurationMillis,
                    maxWaitDurationMillis
            );
        }

        @Override
        public synchronized void reset() {
            tickets.clear();
            active.clear();
            nextSequence = 0;
            issued = 0;
            admitted = 0;
            completed = 0;
            downstreamRejected = 0;
            bypassRejected = 0;
            expiredTickets = 0;
            expiredAdmissions = 0;
            maxQueueDepth = 0;
            maxActive = 0;
            fifoViolations = 0;
            lastAdmittedSequence = 0;
            totalWaitDurationMillis = 0;
            maxWaitDurationMillis = 0;
        }

        private void cleanup(Instant now) {
            var expiredTicketIds = tickets.entrySet().stream()
                    .filter(entry -> entry.getValue().admissionToken == null)
                    .filter(entry -> !entry.getValue().expiresAt.isAfter(now))
                    .map(Map.Entry::getKey)
                    .toList();
            expiredTicketIds.forEach(tickets::remove);
            expiredTickets += expiredTicketIds.size();

            var expiredTokens = active.entrySet().stream()
                    .filter(entry -> !entry.getValue().expiresAt.isAfter(now))
                    .map(Map.Entry::getKey)
                    .toList();
            expiredTokens.forEach(token -> {
                var state = active.remove(token);
                tickets.remove(state.ticketId);
            });
            expiredAdmissions += expiredTokens.size();
        }

        private long queueDepth() {
            return tickets.values().stream()
                    .filter(ticket -> ticket.admissionToken == null)
                    .count();
        }

        private Map<String, TicketState> queuedTickets() {
            var queued = new LinkedHashMap<String, TicketState>();
            tickets.entrySet().stream()
                    .filter(entry -> entry.getValue().admissionToken == null)
                    .sorted(Comparator.comparingLong(entry -> entry.getValue().sequence))
                    .forEach(entry -> queued.put(entry.getKey(), entry.getValue()));
            return queued;
        }

        private void admitRequestedTicket(
                String ticketId,
                TicketState ticket,
                Instant now,
                Instant admissionExpiresAt
        ) {
            ticket.admissionToken = ticket.reservedAdmissionToken;
            ticket.admittedAt = now;
            active.put(
                    ticket.reservedAdmissionToken,
                    new ActiveState(ticketId, admissionExpiresAt)
            );
            admitted++;
            if (ticket.sequence < lastAdmittedSequence) {
                fifoViolations++;
            }
            lastAdmittedSequence = ticket.sequence;
            var waited = Duration.between(ticket.issuedAt, now).toMillis();
            totalWaitDurationMillis += waited;
            maxWaitDurationMillis = Math.max(maxWaitDurationMillis, waited);
            maxActive = Math.max(maxActive, active.size());
        }

        private static final class TicketState {
            private final long sequence;
            private final String reservedAdmissionToken;
            private final Instant issuedAt;
            private final Instant expiresAt;
            private String admissionToken;
            private Instant admittedAt;
            private boolean claimed;

            private TicketState(
                    long sequence,
                    String reservedAdmissionToken,
                    Instant issuedAt,
                    Instant expiresAt
            ) {
                this.sequence = sequence;
                this.reservedAdmissionToken = reservedAdmissionToken;
                this.issuedAt = issuedAt;
                this.expiresAt = expiresAt;
            }
        }

        private record ActiveState(String ticketId, Instant expiresAt) {
        }
    }
}
