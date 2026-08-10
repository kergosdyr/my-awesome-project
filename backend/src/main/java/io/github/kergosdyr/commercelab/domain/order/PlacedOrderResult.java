package io.github.kergosdyr.commercelab.domain.order;

import java.util.List;

public record PlacedOrderResult(
        OrderEntity order,
        List<OrderLineEntity> lines
) {

    public PlacedOrderResult {
        lines = List.copyOf(lines);
    }

    public static PlacedOrderResult from(OrderEntity order) {
        return new PlacedOrderResult(order, order.snapshotLines());
    }
}
