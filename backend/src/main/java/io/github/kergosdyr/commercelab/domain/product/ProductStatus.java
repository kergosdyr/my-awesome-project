package io.github.kergosdyr.commercelab.domain.product;

public enum ProductStatus {
    ACTIVE,
    SOLD_OUT,
    INACTIVE;

    public boolean isPurchasable() {
        return this == ACTIVE;
    }
}
