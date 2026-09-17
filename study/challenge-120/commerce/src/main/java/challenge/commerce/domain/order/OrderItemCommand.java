package challenge.commerce.domain.order;

import challenge.commerce.support.BusinessException;

public record OrderItemCommand(long optionId, int quantity, long displayedUnitPrice) {
    public OrderItemCommand {
        if (optionId <= 0 || quantity < 1 || quantity > 5 || displayedUnitPrice <= 0) {
            throw BusinessException.invalid("옵션, 수량(1~5개), 가격을 확인해 주세요.");
        }
    }
}
