package io.github.kergosdyr.commercelab.domain.cache;

import java.math.BigDecimal;

import io.github.kergosdyr.commercelab.domain.product.ProductEntity;
import io.github.kergosdyr.commercelab.domain.product.ProductStatus;

public record CachedProduct(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        ProductStatus status
) {

    public static CachedProduct fromEntity(ProductEntity product) {
        return new CachedProduct(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getStatus()
        );
    }
}
