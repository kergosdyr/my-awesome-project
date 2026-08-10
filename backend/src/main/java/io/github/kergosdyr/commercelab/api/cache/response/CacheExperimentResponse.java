package io.github.kergosdyr.commercelab.api.cache.response;

import java.math.BigDecimal;

import io.github.kergosdyr.commercelab.domain.cache.CacheExperimentResult;
import io.github.kergosdyr.commercelab.domain.cache.CacheStrategy;
import io.github.kergosdyr.commercelab.domain.cache.CachedProduct;
import io.github.kergosdyr.commercelab.domain.product.ProductStatus;

public record CacheExperimentResponse(
        CacheStrategy strategy,
        CacheExperimentResult.Outcome outcome,
        Product product
) {

    public static CacheExperimentResponse fromResult(CacheExperimentResult result) {
        return new CacheExperimentResponse(
                result.strategy(),
                result.outcome(),
                Product.fromCachedProduct(result.product())
        );
    }

    public record Product(
            Long id,
            String sku,
            String name,
            String description,
            BigDecimal price,
            int stockQuantity,
            ProductStatus status
    ) {

        private static Product fromCachedProduct(CachedProduct product) {
            return new Product(
                    product.id(),
                    product.sku(),
                    product.name(),
                    product.description(),
                    product.price(),
                    product.stockQuantity(),
                    product.status()
            );
        }
    }
}
