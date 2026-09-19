package challenge.commerce.api.response;

import challenge.commerce.domain.order.OrderPageResult;
import java.util.List;

public record OrderPageResponse(List<OrderDetailsResponse> entries, int page, int size, boolean hasNext) {
    public static OrderPageResponse from(OrderPageResult result) {
        return new OrderPageResponse(
                result.entries().stream().map(OrderDetailsResponse::from).toList(),
                result.page(),
                result.size(),
                result.hasNext());
    }
}
