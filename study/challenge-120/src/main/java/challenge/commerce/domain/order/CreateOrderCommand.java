package challenge.commerce.domain.order;

import challenge.commerce.support.BusinessException;

/** HTTP 표현과 독립적인 주문 생성 입력. */
public record CreateOrderCommand(long optionId, int quantity, long displayedUnitPrice) {
    public CreateOrderCommand {
        if (optionId <= 0 || quantity < 1 || quantity > 5 || displayedUnitPrice <= 0) {
            throw BusinessException.invalid("옵션과 수량(1~5개)을 확인해 주세요.");
        }
    }
}
