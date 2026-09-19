package challenge.commerce.api;

import challenge.commerce.support.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiErrors {
    public record ErrorResponse(String code, String message) {}

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> business(BusinessException e) {
        int status =
                switch (e.reason()) {
                    case NOT_FOUND -> 404;
                    case CONFLICT -> 409;
                    case INVALID_INPUT -> 400;
                };
        return ResponseEntity.status(status).body(new ErrorResponse("BUSINESS_ERROR", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalid() {
        return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST", "옵션·수량(1~5개)·화면 표시 가격을 확인해 주세요."));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    ResponseEntity<ErrorResponse> unfinished() {
        return ResponseEntity.status(501).body(new ErrorResponse("NOT_IMPLEMENTED", "요청한 기능이 아직 구현되지 않았습니다."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> invariant() {
        return ResponseEntity.status(409)
                .body(new ErrorResponse("PAYMENT_CONFLICT", "결제 기록을 갱신하지 못했습니다. 기존 주문 상태를 확인해 주세요."));
    }
}
