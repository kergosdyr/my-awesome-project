package challenge.commerce.api.response;

import challenge.commerce.domain.payment.PaymentResult;

public record PaymentResponse(String status, String approvalId) {
    public static PaymentResponse from(PaymentResult result) {
        return new PaymentResponse(result.status().name(), result.approvalId());
    }
}
