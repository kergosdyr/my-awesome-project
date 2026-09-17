package challenge.commerce.domain.pricing;

/** 기존 주문에서 읽은 품목별 가격 입력. optionId로 품목을 구별한다. */
public record CouponLineCommand(long optionId, long unitPrice, int quantity) {}
