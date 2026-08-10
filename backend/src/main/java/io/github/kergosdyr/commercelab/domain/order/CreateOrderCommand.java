package io.github.kergosdyr.commercelab.domain.order;

import java.util.HashSet;
import java.util.List;

import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;

public record CreateOrderCommand(
        String customerName,
        List<Line> lines
) {

    public static final int MAX_DISTINCT_LINES = 20;

    public CreateOrderCommand {
        if (customerName == null || customerName.isBlank()) {
            throw new ApiException(ErrorType.INVALID_ORDER_LINES, "주문자 이름을 입력해 주세요.");
        }
        if (lines == null || lines.isEmpty()) {
            throw new ApiException(ErrorType.INVALID_ORDER_LINES, "상품을 한 개 이상 선택해 주세요.");
        }

        var distinctProductIds = new HashSet<Long>();
        for (var line : lines) {
            if (line == null) {
                throw new ApiException(ErrorType.INVALID_ORDER_LINES, "주문 상품 구성을 확인해 주세요.");
            }
            if (!distinctProductIds.add(line.productId())) {
                throw new ApiException(ErrorType.INVALID_ORDER_LINES, "같은 상품은 한 주문에 한 번만 담아 주세요.");
            }
        }
        if (distinctProductIds.size() > MAX_DISTINCT_LINES) {
            throw new ApiException(
                    ErrorType.INVALID_ORDER_LINES,
                    "한 주문에는 서로 다른 상품을 최대 " + MAX_DISTINCT_LINES + "개까지 담을 수 있습니다."
            );
        }
        lines = List.copyOf(lines);
    }

    public List<Long> productIdsInLockOrder() {
        return lines.stream()
                .map(Line::productId)
                .sorted()
                .toList();
    }

    public record Line(
            Long productId,
            int quantity
    ) {

        public Line {
            if (productId == null || productId <= 0) {
                throw new ApiException(ErrorType.INVALID_ORDER_LINES, "상품 ID를 확인해 주세요.");
            }
            if (quantity <= 0) {
                throw new ApiException(ErrorType.INVALID_ORDER_LINES, "상품 수량은 1개 이상이어야 합니다.");
            }
        }
    }
}
