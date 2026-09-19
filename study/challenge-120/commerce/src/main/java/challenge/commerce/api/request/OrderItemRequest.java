package challenge.commerce.api.request;

import challenge.commerce.domain.order.OrderItemCommand;
import jakarta.validation.constraints.*;

public record OrderItemRequest(
        @Positive long optionId,
        @Min(1) @Max(5) int quantity,
        @NotNull @Positive Long displayedUnitPrice) {
    public OrderItemCommand toCommand() {
        return new OrderItemCommand(optionId, quantity, displayedUnitPrice);
    }
}
