package io.github.kergosdyr.commercelab.api.eventlab.request;

import java.util.List;

import io.github.kergosdyr.commercelab.domain.order.CreateOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateLabOrderRequest(
        @NotBlank(message = "주문자 이름을 입력해 주세요.")
        @Size(max = 40, message = "주문자 이름은 40자 이하여야 합니다.")
        String customerName,

        @NotEmpty(message = "상품을 한 개 이상 선택해 주세요.")
        @Size(max = CreateOrderCommand.MAX_DISTINCT_LINES, message = "한 주문에는 상품을 최대 20개까지 담을 수 있습니다.")
        List<@NotNull(message = "주문 상품 구성을 확인해 주세요.") @Valid LineRequest> lines
) {

    public CreateOrderCommand toCommand() {
        var commandLines = lines.stream()
                .map(LineRequest::toCommand)
                .toList();
        return new CreateOrderCommand(customerName.trim(), commandLines);
    }

    public record LineRequest(
            @NotNull(message = "상품 ID를 입력해 주세요.")
            @Positive(message = "상품 ID를 확인해 주세요.")
            Long productId,

            @NotNull(message = "상품 수량을 입력해 주세요.")
            @Positive(message = "상품 수량은 1개 이상이어야 합니다.")
            @Max(value = 99, message = "한 상품은 최대 99개까지 주문할 수 있습니다.")
            Integer quantity
    ) {

        CreateOrderCommand.Line toCommand() {
            return new CreateOrderCommand.Line(productId, quantity);
        }
    }
}
