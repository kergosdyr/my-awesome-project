package io.github.kergosdyr.commercelab.domain.cache;

import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;

public record CacheProductQuery(
        Long productId,
        CacheStrategy strategy
) {

    public CacheProductQuery {
        if (productId == null || productId <= 0) {
            throw new ApiException(ErrorType.VALIDATION_ERROR, "상품 ID를 확인해 주세요.");
        }
        if (strategy == null) {
            throw new ApiException(ErrorType.VALIDATION_ERROR, "캐시 전략을 확인해 주세요.");
        }
    }
}
