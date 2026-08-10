package io.github.kergosdyr.commercelab.domain.waitingroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

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
