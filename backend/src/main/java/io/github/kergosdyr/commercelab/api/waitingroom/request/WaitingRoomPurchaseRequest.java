package io.github.kergosdyr.commercelab.api.waitingroom.request;

import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WaitingRoomPurchaseRequest(
        @NotBlank(message = "대기표 ID를 입력해 주세요.")
        @Size(max = 64, message = "대기표 ID를 확인해 주세요.")
        String ticketId,

        @NotBlank(message = "입장 토큰을 입력해 주세요.")
        @Size(max = 64, message = "입장 토큰을 확인해 주세요.")
        String admissionToken
) {

    public WaitingRoomCommand.Purchase toCommand() {
        return new WaitingRoomCommand.Purchase(ticketId.trim(), admissionToken.trim());
    }
}
