package challenge.commerce.api.request;

import challenge.commerce.domain.order.CreateOrderCommand;
import jakarta.validation.constraints.*;

public record CreateOrderRequest(
        @Positive long optionId,
        @Min(1) @Max(5) int quantity,
        @NotNull @Positive Long displayedUnitPrice) {
    public CreateOrderCommand toCommand() {
        return new CreateOrderCommand(optionId, quantity, displayedUnitPrice);
    }
}
