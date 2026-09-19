package challenge.commerce.api;

import challenge.commerce.api.response.PaymentResponse;
import challenge.commerce.domain.payment.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/orders/{id}/payments")
    public ResponseEntity<PaymentResponse> pay(@PathVariable("id") long id) {
        var result = paymentService.pay(id);
        return ResponseEntity.status(result.status() == PaymentResult.Status.PAID ? 200 : 202)
                .body(PaymentResponse.from(result));
    }

    @PostMapping("/api/payments/notifications")
    public NotificationResponse notify(@Valid @RequestBody NotificationRequest request) {
        return new NotificationResponse(paymentService.onNotification(request.toReceipt()));
    }

    public record NotificationResponse(boolean accepted) {}

    public record NotificationRequest(
            @NotBlank String key,
            @Positive long orderId,
            @Positive long amount,
            @NotNull PaymentGateway.Status status,
            @NotBlank String approvalId) {
        PaymentGateway.Receipt toReceipt() {
            return new PaymentGateway.Receipt(key, orderId, amount, status, approvalId);
        }
    }
}
