package io.github.kergosdyr.commercelab.domain.waitingroom;

import java.time.Clock;
import java.util.UUID;

import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class WaitingRoomService {

    private final WaitingRoomRepository waitingRoomRepository;
    private final FlashSaleClient flashSaleClient;
    private final WaitingRoomPolicy policy;
    private final Clock clock;

    public WaitingRoomService(
            WaitingRoomRepository waitingRoomRepository,
            FlashSaleClient flashSaleClient,
            WaitingRoomPolicy policy,
            Clock clock
    ) {
        this.waitingRoomRepository = waitingRoomRepository;
        this.flashSaleClient = flashSaleClient;
        this.policy = policy;
        this.clock = clock;
    }

    public WaitingRoomResult.Purchase purchaseDirect() {
        var attempt = flashSaleClient.tryPurchase();
        if (!attempt.accepted()) {
            throw new ApiException(ErrorType.WAITING_ROOM_CAPACITY_EXCEEDED);
        }

        return new WaitingRoomResult.Purchase(
                "DIRECT",
                "ACCEPTED",
                null,
                clock.instant(),
                attempt.processingDurationMillis()
        );
    }

    public WaitingRoomResult.Ticket issueTicket() {
        var issuedAt = clock.instant();
        var expiresAt = issuedAt.plus(policy.ticketTtl());
        var ticketId = UUID.randomUUID().toString();
        var enqueued = waitingRoomRepository.enqueue(ticketId, issuedAt, expiresAt);

        return new WaitingRoomResult.Ticket(
                ticketId,
                "QUEUED",
                enqueued.position(),
                issuedAt,
                expiresAt,
                policy.pollInterval().toMillis()
        );
    }

    public WaitingRoomResult.Poll pollTicket(String ticketId) {
        var now = clock.instant();
        var admissionExpiresAt = now.plus(policy.admissionTtl());
        var decision = waitingRoomRepository.poll(
                ticketId,
                UUID.randomUUID().toString(),
                now,
                admissionExpiresAt,
                policy.maxConcurrency()
        );

        if (decision.status() == WaitingRoomRepository.PollStatus.EXPIRED) {
            throw new ApiException(ErrorType.WAITING_ROOM_TICKET_EXPIRED);
        }

        return new WaitingRoomResult.Poll(
                ticketId,
                decision.status().name(),
                decision.position(),
                decision.admissionToken(),
                decision.admissionExpiresAt(),
                decision.waitDurationMillis(),
                decision.status() == WaitingRoomRepository.PollStatus.QUEUED
                        ? policy.pollInterval().toMillis()
                        : 0
        );
    }

    public WaitingRoomResult.Purchase purchaseWithAdmission(WaitingRoomCommand.Purchase command) {
        var now = clock.instant();
        var claimStatus = waitingRoomRepository.claim(
                command.ticketId(),
                command.admissionToken(),
                now,
                now.plus(policy.processingLeaseTtl())
        );
        if (claimStatus == WaitingRoomRepository.ClaimStatus.EXPIRED) {
            throw new ApiException(ErrorType.WAITING_ROOM_TICKET_EXPIRED);
        }
        if (claimStatus == WaitingRoomRepository.ClaimStatus.INVALID) {
            throw new ApiException(ErrorType.WAITING_ROOM_ADMISSION_REQUIRED);
        }

        var completed = false;
        try {
            var attempt = flashSaleClient.tryPurchase();
            if (!attempt.accepted()) {
                throw new ApiException(ErrorType.WAITING_ROOM_CAPACITY_EXCEEDED);
            }
            completed = true;
            return new WaitingRoomResult.Purchase(
                    "WAITING_ROOM",
                    "ACCEPTED",
                    command.ticketId(),
                    clock.instant(),
                    attempt.processingDurationMillis()
            );
        } finally {
            waitingRoomRepository.release(command.ticketId(), command.admissionToken(), completed);
        }
    }

    public WaitingRoomResult.Metrics metrics() {
        return new WaitingRoomResult.Metrics(
                new WaitingRoomResult.Policy(
                        policy.maxConcurrency(),
                        policy.workDuration().toMillis(),
                        policy.ticketTtl().toMillis(),
                        policy.admissionTtl().toMillis(),
                        policy.processingLeaseTtl().toMillis(),
                        policy.pollInterval().toMillis()
                ),
                flashSaleClient.metrics(),
                waitingRoomRepository.metrics(clock.instant())
        );
    }

    public WaitingRoomResult.Metrics reset() {
        waitingRoomRepository.reset();
        flashSaleClient.reset();
        return metrics();
    }
}
