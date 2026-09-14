package challenge.lab.api;

import challenge.payment.PaymentGateway;
import challenge.payment.PaymentQueryService;
import challenge.payment.PaymentResult;
import challenge.payment.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 제공 HTTP 경계. 결제 로직/DB 조회를 직접 수행하지 않고 facade를 호출한다. */
@RestController
public class PaymentController {
    private final PaymentService payments;
    private final PaymentQueryService queries;

    public PaymentController(PaymentService payments, PaymentQueryService queries) {
        this.payments = payments;
        this.queries = queries;
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PayRequest request) {
        return PaymentResponse.http(payments.pay(request.reservationId(), request.amount()));
    }

    @GetMapping("/payments/{reservationId}")
    public ResponseEntity<PaymentResponse> find(@PathVariable("reservationId") long reservationId) {
        return PaymentResponse.http(queries.find(reservationId));
    }

    @PostMapping("/payments/notifications")
    public NotificationResponse notify(@Valid @RequestBody NotificationRequest request) {
        return new NotificationResponse(payments.onNotification(request.toReceipt()));
    }

    public record PayRequest(@Positive long reservationId, @Positive long amount) {}

    public record NotificationRequest(
            @NotBlank String key,
            @Positive long reservationId,
            @Positive long amount,
            @NotNull PaymentGateway.Status status,
            @NotBlank String approvalId) {
        PaymentGateway.Receipt toReceipt() {
            return new PaymentGateway.Receipt(key, reservationId, amount, status, approvalId);
        }
    }

    public record NotificationResponse(boolean accepted) {}

    public record PaymentResponse(String status, String approvalId) {
        static ResponseEntity<PaymentResponse> http(PaymentResult result) {
            int code =
                    switch (result.status()) {
                        case PAID -> 200;
                        case PENDING -> 202;
                        case NOT_FOUND -> 404;
                        case CONFLICT -> 409;
                    };
            return ResponseEntity.status(code)
                    .body(new PaymentResponse(result.status().name(), result.approvalId()));
        }
    }
}
