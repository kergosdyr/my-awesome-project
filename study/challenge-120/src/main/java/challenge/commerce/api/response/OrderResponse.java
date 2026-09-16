package challenge.commerce.api.response;

import challenge.commerce.infra.db.OrderEntity;
import java.time.Instant;

public record OrderResponse(
        Long id,
        long optionId,
        String productName,
        String optionName,
        String image,
        long unitPrice,
        int quantity,
        long totalAmount,
        Instant createdAt) {
    public static OrderResponse from(OrderEntity order) {
        return new OrderResponse(
                order.id(),
                order.optionId(),
                order.productName(),
                order.optionName(),
                order.image(),
                order.unitPrice(),
                order.quantity(),
                order.totalAmount(),
                order.createdAt());
    }
}
