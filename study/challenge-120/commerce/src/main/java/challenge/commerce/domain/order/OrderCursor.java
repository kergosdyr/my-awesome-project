package challenge.commerce.domain.order;

import challenge.commerce.support.BusinessException;
import java.time.Instant;

/** 마지막으로 읽은 주문의 정렬 값. 문자열 조립·해석 없이 그대로 전달한다. */
public record OrderCursor(Instant createdAt, long id) {
    public OrderCursor {
        if (createdAt == null || id <= 0) {
            throw BusinessException.invalid("주문 시각과 양수 주문 ID가 필요합니다.");
        }
    }
}
