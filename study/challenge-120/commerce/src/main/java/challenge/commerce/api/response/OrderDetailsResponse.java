package challenge.commerce.api.response;

import challenge.commerce.domain.order.OrderDetailsResult;

public record OrderDetailsResponse(OrderResponse order, String paymentStatus, String approvalId) {
    public static OrderDetailsResponse from(OrderDetailsResult result) {
        var payment = result.payment();
        return new OrderDetailsResponse(
                OrderResponse.from(result.order()),
                payment == null ? "UNPAID" : payment.status().name(),
                payment == null ? null : payment.approvalId());
    }
}
