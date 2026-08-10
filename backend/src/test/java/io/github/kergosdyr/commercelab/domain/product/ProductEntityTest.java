package io.github.kergosdyr.commercelab.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.junit.jupiter.api.Test;

class ProductEntityTest {

    @Test
    void reservesStockAndMarksTheProductSoldOut() {
        var product = productWith(3, ProductStatus.ACTIVE);

        product.reserve(3);

        assertThat(product.getStockQuantity()).isZero();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @Test
    void rejectsAReservationThatExceedsStock() {
        var product = productWith(2, ProductStatus.ACTIVE);

        assertThatThrownBy(() -> product.reserve(3))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorType()).isEqualTo(ErrorType.INSUFFICIENT_STOCK)
                );
        assertThat(product.getStockQuantity()).isEqualTo(2);
    }

    @Test
    void rejectsAReservationForAProductThatIsNotForSale() {
        var product = productWith(0, ProductStatus.SOLD_OUT);

        assertThatThrownBy(() -> product.reserve(1))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorType()).isEqualTo(ErrorType.PRODUCT_NOT_FOR_SALE)
                );
    }

    private ProductEntity productWith(int stockQuantity, ProductStatus status) {
        return new ProductEntity(
                1L,
                "TEST-SKU",
                "테스트 상품",
                "테스트 설명",
                BigDecimal.valueOf(10_000),
                stockQuantity,
                status
        );
    }
}
