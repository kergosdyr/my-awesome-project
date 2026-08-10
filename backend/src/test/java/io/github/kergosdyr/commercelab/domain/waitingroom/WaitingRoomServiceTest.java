package io.github.kergosdyr.commercelab.domain.waitingroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomTestFixtures.FakeFlashSaleClient;
import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomTestFixtures.FakeWaitingRoomRepository;
import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomTestFixtures.MutableClock;
import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomTestFixtures.TestPolicy;
import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WaitingRoomServiceTest {

    private MutableClock clock;
    private FakeWaitingRoomRepository repository;
    private FakeFlashSaleClient flashSaleClient;
    private WaitingRoomService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-08-11T00:00:00Z"));
        repository = new FakeWaitingRoomRepository();
        flashSaleClient = new FakeFlashSaleClient();
        service = new WaitingRoomService(
                repository,
                flashSaleClient,
                TestPolicy.oneSlot(),
                clock
        );
    }

    @Test
    void admitsTicketsInFifoOrderAndNeverExceedsTheActiveLimit() {
        var first = service.issueTicket();
        var second = service.issueTicket();

        assertThat(service.pollTicket(second.ticketId()).status()).isEqualTo("QUEUED");
        var firstAdmission = service.pollTicket(first.ticketId());
        assertThat(firstAdmission.status()).isEqualTo("ADMITTED");
        assertThat(service.pollTicket(second.ticketId()).status()).isEqualTo("QUEUED");

        service.purchaseWithAdmission(new WaitingRoomCommand.Purchase(
                first.ticketId(),
                firstAdmission.admissionToken()
        ));
        clock.advance(Duration.ofMillis(25));
        var secondAdmission = service.pollTicket(second.ticketId());

        assertThat(secondAdmission.status()).isEqualTo("ADMITTED");
        assertThat(secondAdmission.waitDurationMillis()).isEqualTo(25);
        assertThat(service.metrics().waitingRoom().maxActive()).isEqualTo(1);
        assertThat(service.metrics().waitingRoom().fifoViolations()).isZero();
    }

    @Test
    void promotesOnlyFreshQueueHeadsInStrictFifoOrder() {
        var fourSlotPolicy = new TestPolicy(
                4,
                Duration.ofMillis(1),
                Duration.ofSeconds(30),
                Duration.ofSeconds(10),
                Duration.ofSeconds(5),
                Duration.ofMillis(100),
                Duration.ofMillis(10)
        );
        var fourSlotService = new WaitingRoomService(
                repository,
                flashSaleClient,
                fourSlotPolicy,
                clock
        );
        var first = fourSlotService.issueTicket();
        var second = fourSlotService.issueTicket();
        var third = fourSlotService.issueTicket();
        var fourth = fourSlotService.issueTicket();
        var fifth = fourSlotService.issueTicket();

        var fifthPoll = fourSlotService.pollTicket(fifth.ticketId());
        var fourthPoll = fourSlotService.pollTicket(fourth.ticketId());
        var thirdPoll = fourSlotService.pollTicket(third.ticketId());
        var secondPoll = fourSlotService.pollTicket(second.ticketId());
        var metricsBeforeHeadPoll = fourSlotService.metrics().waitingRoom();

        assertThat(fifthPoll.status()).isEqualTo("QUEUED");
        assertThat(fifthPoll.position()).isEqualTo(5);
        assertThat(fourthPoll.status()).isEqualTo("QUEUED");
        assertThat(thirdPoll.status()).isEqualTo("QUEUED");
        assertThat(secondPoll.status()).isEqualTo("QUEUED");
        assertThat(metricsBeforeHeadPoll.currentActive()).isZero();

        var firstPoll = fourSlotService.pollTicket(first.ticketId());
        var metricsAfterBatch = fourSlotService.metrics().waitingRoom();

        assertThat(firstPoll.status()).isEqualTo("ADMITTED");
        assertThat(metricsAfterBatch.currentActive()).isEqualTo(4);
        assertThat(metricsAfterBatch.maxActive()).isEqualTo(4);
        assertThat(metricsAfterBatch.lastAdmittedSequence()).isEqualTo(4);
        assertThat(metricsAfterBatch.fifoViolations()).isZero();
        assertThat(fourSlotService.pollTicket(fourth.ticketId()).status()).isEqualTo("ADMITTED");
        assertThat(fourSlotService.pollTicket(third.ticketId()).status()).isEqualTo("ADMITTED");
        assertThat(fourSlotService.pollTicket(second.ticketId()).status()).isEqualTo("ADMITTED");
        assertThat(fourSlotService.pollTicket(first.ticketId()).status()).isEqualTo("ADMITTED");

        clock.advance(Duration.ofMillis(101));
        fourSlotService.purchaseWithAdmission(new WaitingRoomCommand.Purchase(
                first.ticketId(),
                firstPoll.admissionToken()
        ));

        var fifthAfterFreshPoll = fourSlotService.pollTicket(fifth.ticketId());
        assertThat(fifthAfterFreshPoll.status()).isEqualTo("ADMITTED");
        assertThat(fourSlotService.metrics().waitingRoom().lastAdmittedSequence()).isEqualTo(5);
        assertThat(fourSlotService.metrics().waitingRoom().fifoViolations()).isZero();
    }

    @Test
    void neverSkipsAStaleDisconnectedQueueHead() {
        var first = service.issueTicket();
        var second = service.issueTicket();

        assertThat(service.pollTicket(second.ticketId()).status()).isEqualTo("QUEUED");
        clock.advance(Duration.ofMillis(101));
        var secondFreshPoll = service.pollTicket(second.ticketId());

        assertThat(secondFreshPoll.status()).isEqualTo("QUEUED");
        assertThat(secondFreshPoll.position()).isEqualTo(2);
        assertThat(service.metrics().waitingRoom().currentActive()).isZero();
        assertThat(service.metrics().waitingRoom().lastAdmittedSequence()).isZero();

        assertThat(service.pollTicket(first.ticketId()).status()).isEqualTo("ADMITTED");
        assertThat(service.metrics().waitingRoom().lastAdmittedSequence()).isEqualTo(1);
        assertThat(service.metrics().waitingRoom().fifoViolations()).isZero();
    }

    @Test
    void advisesPollingByQueuePositionAndCapsFarAndOverflowingPositions() {
        var positionAwarePolicy = new TestPolicy(
                4,
                Duration.ofMillis(50),
                Duration.ofSeconds(30),
                Duration.ofSeconds(10),
                Duration.ofSeconds(5),
                Duration.ofMillis(100),
                Duration.ofSeconds(1)
        );
        var positionAwareService = new WaitingRoomService(
                repository,
                flashSaleClient,
                positionAwarePolicy,
                clock
        );
        var tickets = new ArrayList<WaitingRoomResult.Ticket>();
        for (var position = 1; position <= 165; position++) {
            tickets.add(positionAwareService.issueTicket());
        }

        assertThat(tickets.get(0).pollAfterMillis()).isEqualTo(10);
        assertThat(tickets.get(3).pollAfterMillis()).isEqualTo(10);
        assertThat(tickets.get(4).pollAfterMillis()).isEqualTo(25);
        assertThat(tickets.get(160).pollAfterMillis()).isEqualTo(1000);
        assertThat(positionAwareService.calculatePollAfterMillis(Long.MAX_VALUE)).isEqualTo(1000);

        positionAwareService.pollTicket(tickets.get(3).ticketId());
        positionAwareService.pollTicket(tickets.get(2).ticketId());
        positionAwareService.pollTicket(tickets.get(1).ticketId());
        var admitted = positionAwareService.pollTicket(tickets.get(0).ticketId());
        var newHead = positionAwareService.pollTicket(tickets.get(4).ticketId());
        var fifthQueuedPosition = positionAwareService.pollTicket(tickets.get(8).ticketId());
        var farQueuedPosition = positionAwareService.pollTicket(tickets.get(164).ticketId());

        assertThat(admitted.pollAfterMillis()).isZero();
        assertThat(newHead.position()).isEqualTo(1);
        assertThat(newHead.pollAfterMillis()).isEqualTo(10);
        assertThat(fifthQueuedPosition.position()).isEqualTo(5);
        assertThat(fifthQueuedPosition.pollAfterMillis()).isEqualTo(25);
        assertThat(farQueuedPosition.position()).isEqualTo(161);
        assertThat(farQueuedPosition.pollAfterMillis()).isEqualTo(1000);
    }

    @Test
    void rejectsABypassTokenBeforeCallingTheDownstream() {
        var ticket = service.issueTicket();
        service.pollTicket(ticket.ticketId());

        assertThatThrownBy(() -> service.purchaseWithAdmission(
                new WaitingRoomCommand.Purchase(ticket.ticketId(), "forged-token")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.errorType()).isEqualTo(ErrorType.WAITING_ROOM_ADMISSION_REQUIRED)
        );
        assertThat(flashSaleClient.metrics().accepted()).isZero();
        assertThat(service.metrics().waitingRoom().bypassRejected()).isEqualTo(1);
    }

    @Test
    void expiresAnUnpolledTicketAndReleasesItsQueuePosition() {
        var ticket = service.issueTicket();
        clock.advance(Duration.ofSeconds(31));

        assertThatThrownBy(() -> service.pollTicket(ticket.ticketId()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorType()).isEqualTo(ErrorType.WAITING_ROOM_TICKET_EXPIRED)
                );
        assertThat(service.metrics().waitingRoom().currentQueueDepth()).isZero();
        assertThat(service.metrics().waitingRoom().expiredTickets()).isEqualTo(1);
    }

    @Test
    void expiresAnUnusedAdmissionAndReopensTheActiveSlot() {
        var first = service.issueTicket();
        var firstAdmission = service.pollTicket(first.ticketId());
        var second = service.issueTicket();

        assertThat(service.pollTicket(second.ticketId()).status()).isEqualTo("QUEUED");

        clock.advance(Duration.ofSeconds(11));
        var secondAdmission = service.pollTicket(second.ticketId());

        assertThat(secondAdmission.status()).isEqualTo("ADMITTED");
        assertThatThrownBy(() -> service.purchaseWithAdmission(new WaitingRoomCommand.Purchase(
                first.ticketId(),
                firstAdmission.admissionToken()
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.errorType()).isEqualTo(ErrorType.WAITING_ROOM_TICKET_EXPIRED)
        );
        assertThat(service.metrics().waitingRoom().expiredAdmissions()).isEqualTo(1);
        assertThat(service.metrics().waitingRoom().maxActive()).isEqualTo(1);
    }

    @Test
    void directModeReturnsAVisibleCapacityRejection() {
        flashSaleClient.rejectRequests();

        assertThatThrownBy(service::purchaseDirect)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorType()).isEqualTo(ErrorType.WAITING_ROOM_CAPACITY_EXCEEDED)
                );
        assertThat(flashSaleClient.metrics().rejected()).isEqualTo(1);
    }
}
