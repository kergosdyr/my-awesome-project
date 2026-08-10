package io.github.kergosdyr.commercelab.api.order.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import io.github.kergosdyr.commercelab.domain.order.OrderLineEntity;
import io.github.kergosdyr.commercelab.domain.order.OrderStatus;
import io.github.kergosdyr.commercelab.domain.order.PlacedOrderResult;

public record CreateOrderResponse(
        String orderNumber,
        String customerName,
        BigDecimal totalAmount,
        OrderStatus status,
        Instant createdAt,
        List<LineResponse> lines
) {

    public static CreateOrderResponse fromResult(PlacedOrderResult result) {
        var order = result.order();
        var lines = result.lines().stream()
                .map(LineResponse::fromEntity)
                .toList();

        return new CreateOrderResponse(
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                lines
        );
    }

    public record LineResponse(
            Long productId,
            String sku,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineAmount
    ) {

        static LineResponse fromEntity(OrderLineEntity line) {
            return new LineResponse(
                    line.getProductId(),
                    line.getProductSku(),
                    line.getProductName(),
                    line.getUnitPrice(),
                    line.getQuantity(),
                    line.getLineAmount()
            );
        }
    }
}
