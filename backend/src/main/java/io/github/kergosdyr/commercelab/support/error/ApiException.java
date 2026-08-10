package io.github.kergosdyr.commercelab.support.error;

public class ApiException extends RuntimeException {

    private final ErrorType errorType;

    public ApiException(ErrorType errorType) {
        super(errorType.message());
        this.errorType = errorType;
    }

    public ApiException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType errorType() {
        return errorType;
    }
}
