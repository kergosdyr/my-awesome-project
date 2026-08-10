package io.github.kergosdyr.commercelab.api.product.response;

import java.math.BigDecimal;

import io.github.kergosdyr.commercelab.domain.product.ProductEntity;
import io.github.kergosdyr.commercelab.domain.product.ProductStatus;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        ProductStatus status
) {

    public static ProductResponse fromEntity(ProductEntity product) {
        return new ProductResponse(
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
