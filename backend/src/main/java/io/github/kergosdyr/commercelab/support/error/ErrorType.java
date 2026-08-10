package io.github.kergosdyr.commercelab.support.error;

import org.springframework.http.HttpStatus;

public enum ErrorType {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값을 확인해 주세요."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다."),
    INVALID_ORDER_LINES(HttpStatus.BAD_REQUEST, "주문 상품 구성을 확인해 주세요."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
    PRODUCT_NOT_FOR_SALE(HttpStatus.CONFLICT, "현재 구매할 수 없는 상품입니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "상품 재고가 부족합니다."),
    WAITING_ROOM_CAPACITY_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "구매 처리 용량을 초과했습니다."),
    WAITING_ROOM_TICKET_EXPIRED(HttpStatus.GONE, "대기표 또는 입장 권한이 만료됐습니다."),
    WAITING_ROOM_ADMISSION_REQUIRED(HttpStatus.FORBIDDEN, "유효한 입장 권한이 필요합니다."),
    WAITING_ROOM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "대기열을 확인할 수 없어 구매를 중단했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;

    ErrorType(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
