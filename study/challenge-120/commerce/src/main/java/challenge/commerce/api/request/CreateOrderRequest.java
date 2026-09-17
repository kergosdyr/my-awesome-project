package challenge.commerce.api.request;

import challenge.commerce.domain.order.CreateOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty @Size(max = CreateOrderCommand.MAX_ITEMS) List<@NotNull @Valid OrderItemRequest> items) {
    public CreateOrderCommand toCommand() {
        return new CreateOrderCommand(
                items.stream().map(OrderItemRequest::toCommand).toList());
    }
}
