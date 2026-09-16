package challenge.commerce.api.response;

import challenge.commerce.domain.order.OrderDetailsResult;

public record OrderDetailsResponse(OrderResponse order, String paymentStatus, String approvalId) {
    public static OrderDetailsResponse from(OrderDetailsResult result) {
        return new OrderDetailsResponse(
                OrderResponse.from(result.order()), result.paymentStatus().name(), result.approvalId());
    }
}
