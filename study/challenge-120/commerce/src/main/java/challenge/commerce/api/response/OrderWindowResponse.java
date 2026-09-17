package challenge.commerce.api.response;

import challenge.commerce.domain.order.OrderWindowResult;
import java.util.List;

public record OrderWindowResponse(List<OrderDetailsResponse> entries, String next) {
    public static OrderWindowResponse from(OrderWindowResult result) {
        return new OrderWindowResponse(
                result.entries().stream().map(OrderDetailsResponse::from).toList(), result.next());
    }
}
