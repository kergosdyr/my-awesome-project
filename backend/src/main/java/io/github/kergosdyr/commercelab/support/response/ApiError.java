package io.github.kergosdyr.commercelab.support.response;

public record ApiError(
        String code,
        String message
) {
}
