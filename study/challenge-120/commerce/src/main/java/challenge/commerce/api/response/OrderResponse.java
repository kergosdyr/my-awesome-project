package challenge.commerce.api.response;

import challenge.commerce.infra.db.OrderEntity;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, List<OrderItemResponse> items, long totalAmount, Instant createdAt) {
    public static OrderResponse from(OrderEntity order) {
        return new OrderResponse(
                order.id(),
                order.items().stream().map(OrderItemResponse::from).toList(),
                order.totalAmount(),
                order.createdAt());
    }
}
