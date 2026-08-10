package io.github.kergosdyr.commercelab.api.waitingroom;

import io.github.kergosdyr.commercelab.api.waitingroom.request.WaitingRoomPurchaseRequest;
import io.github.kergosdyr.commercelab.api.waitingroom.response.WaitingRoomResponse;
import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomService;
import io.github.kergosdyr.commercelab.support.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/labs/waiting-room")
public class WaitingRoomController {

    private final WaitingRoomService waitingRoomService;

    public WaitingRoomController(WaitingRoomService waitingRoomService) {
        this.waitingRoomService = waitingRoomService;
    }

    @PostMapping("/direct")
    public ApiResponse<WaitingRoomResponse.Purchase> purchaseDirect() {
        return ApiResponse.success(
                WaitingRoomResponse.Purchase.fromResult(waitingRoomService.purchaseDirect())
        );
    }

    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse<WaitingRoomResponse.Ticket>> issueTicket() {
        var result = waitingRoomService.issueTicket();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(WaitingRoomResponse.Ticket.fromResult(result)));
    }

    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<ApiResponse<WaitingRoomResponse.Poll>> pollTicket(
            @PathVariable
            @NotBlank(message = "대기표 ID를 입력해 주세요.")
            @Size(max = 64, message = "대기표 ID를 확인해 주세요.")
            String ticketId
    ) {
        var result = waitingRoomService.pollTicket(ticketId);
        var status = result.queued() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status)
                .body(ApiResponse.success(WaitingRoomResponse.Poll.fromResult(result)));
    }

    @PostMapping("/purchase")
    public ApiResponse<WaitingRoomResponse.Purchase> purchaseWithAdmission(
            @Valid @RequestBody WaitingRoomPurchaseRequest request
    ) {
        return ApiResponse.success(
                WaitingRoomResponse.Purchase.fromResult(
                        waitingRoomService.purchaseWithAdmission(request.toCommand())
                )
        );
    }

    @GetMapping("/metrics")
    public ApiResponse<WaitingRoomResponse.Metrics> metrics() {
        return ApiResponse.success(
                WaitingRoomResponse.Metrics.fromResult(waitingRoomService.metrics())
        );
    }

    @PostMapping("/reset")
    public ApiResponse<WaitingRoomResponse.Metrics> reset() {
        return ApiResponse.success(
                WaitingRoomResponse.Metrics.fromResult(waitingRoomService.reset())
        );
    }
}
