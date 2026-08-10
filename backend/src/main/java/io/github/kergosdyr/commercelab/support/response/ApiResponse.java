package io.github.kergosdyr.commercelab.support.response;

import io.github.kergosdyr.commercelab.support.error.ErrorType;

public record ApiResponse<T>(
        T data,
        ApiError error
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null);
    }

    public static ApiResponse<Void> failure(ErrorType errorType, String message) {
        return new ApiResponse<>(null, new ApiError(errorType.name(), message));
    }
}
