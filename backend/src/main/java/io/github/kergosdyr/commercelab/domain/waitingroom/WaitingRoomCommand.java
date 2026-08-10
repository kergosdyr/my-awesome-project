package io.github.kergosdyr.commercelab.domain.waitingroom;

public final class WaitingRoomCommand {

    private WaitingRoomCommand() {
    }

    public record Purchase(String ticketId, String admissionToken) {
    }
}
