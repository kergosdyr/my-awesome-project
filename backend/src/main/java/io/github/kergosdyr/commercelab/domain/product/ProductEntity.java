package io.github.kergosdyr.commercelab.domain.product;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;

import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockQuantity;

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    protected ProductEntity() {
    }

    ProductEntity(
            Long id,
            String sku,
            String name,
            String description,
            BigDecimal price,
            int stockQuantity,
            ProductStatus status
    ) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.status = status;
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new ApiException(ErrorType.INVALID_ORDER_LINES, "상품 수량은 1개 이상이어야 합니다.");
        }
        if (!status.isPurchasable()) {
            throw new ApiException(ErrorType.PRODUCT_NOT_FOR_SALE, name + "은(는) 현재 구매할 수 없습니다.");
        }
        if (stockQuantity < quantity) {
            throw new ApiException(ErrorType.INSUFFICIENT_STOCK, name + "의 재고가 부족합니다.");
        }

        stockQuantity -= quantity;
        if (stockQuantity == 0) {
            status = ProductStatus.SOLD_OUT;
        }
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public ProductStatus getStatus() {
        return status;
    }
}
