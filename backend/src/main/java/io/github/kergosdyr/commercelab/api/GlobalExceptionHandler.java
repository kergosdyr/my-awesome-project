package io.github.kergosdyr.commercelab.api;

import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import io.github.kergosdyr.commercelab.support.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        var errorType = exception.errorType();
        return ResponseEntity.status(errorType.status())
                .body(ApiResponse.failure(errorType, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        var message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorType.VALIDATION_ERROR.message());

        return ResponseEntity.status(ErrorType.VALIDATION_ERROR.status())
                .body(ApiResponse.failure(ErrorType.VALIDATION_ERROR, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.status(ErrorType.VALIDATION_ERROR.status())
                .body(ApiResponse.failure(ErrorType.VALIDATION_ERROR, ErrorType.VALIDATION_ERROR.message()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(ErrorType.MALFORMED_REQUEST.status())
                .body(ApiResponse.failure(ErrorType.MALFORMED_REQUEST, ErrorType.MALFORMED_REQUEST.message()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingResource(NoResourceFoundException exception) {
        return ResponseEntity.status(ErrorType.RESOURCE_NOT_FOUND.status())
                .body(ApiResponse.failure(ErrorType.RESOURCE_NOT_FOUND, ErrorType.RESOURCE_NOT_FOUND.message()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled API exception", exception);
        return ResponseEntity.status(ErrorType.INTERNAL_SERVER_ERROR.status())
                .body(ApiResponse.failure(ErrorType.INTERNAL_SERVER_ERROR, ErrorType.INTERNAL_SERVER_ERROR.message()));
    }
}
