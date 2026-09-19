package challenge.commerce.api.response;

import challenge.commerce.infra.db.OrderItemEntity;

public record OrderItemResponse(
        long optionId,
        String productName,
        String optionName,
        String image,
        long unitPrice,
        int quantity,
        long totalAmount) {
    public static OrderItemResponse from(OrderItemEntity item) {
        return new OrderItemResponse(
                item.optionId(),
                item.productName(),
                item.optionName(),
                item.image(),
                item.unitPrice(),
                item.quantity(),
                item.totalAmount());
    }
}
