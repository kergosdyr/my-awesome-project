package challenge.commerce.domain.order;

import challenge.commerce.support.BusinessException;
import java.util.List;

/** HTTP 표현과 독립적인 주문 생성 입력. */
public record CreateOrderCommand(List<OrderItemCommand> items) {
    public static final int MAX_ITEMS = 20;

    public CreateOrderCommand {
        if (items == null
                || items.isEmpty()
                || items.size() > MAX_ITEMS
                || items.stream().anyMatch(java.util.Objects::isNull)) {
            throw BusinessException.invalid("주문 품목을 1~20개 선택해 주세요.");
        }
        items = List.copyOf(items);
        if (items.stream().map(OrderItemCommand::optionId).distinct().count() != items.size()) {
            throw BusinessException.invalid("같은 옵션은 하나의 품목으로 수량을 지정해 주세요.");
        }
    }
}
