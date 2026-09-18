package challenge.commerce.api.response;

import challenge.commerce.domain.order.OrderWindowResult;
import java.time.Instant;
import java.util.List;

public record OrderWindowResponse(List<OrderDetailsResponse> entries, Next next) {
    public static OrderWindowResponse from(OrderWindowResult result) {
        return new OrderWindowResponse(
                result.entries().stream().map(OrderDetailsResponse::from).toList(),
                result.next() == null
                        ? null
                        : new Next(result.next().createdAt(), result.next().id()));
    }

    public record Next(Instant createdAt, long id) {}
}
