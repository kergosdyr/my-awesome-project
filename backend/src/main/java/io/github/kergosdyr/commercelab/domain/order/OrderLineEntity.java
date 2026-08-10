package io.github.kergosdyr.commercelab.domain.order;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;

import io.github.kergosdyr.commercelab.domain.product.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_lines")
public class OrderLineEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 50)
    private String productSku;

    @Column(nullable = false, length = 120)
    private String productName;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal lineAmount;

    protected OrderLineEntity() {
    }

    static OrderLineEntity create(OrderEntity order, ProductEntity product, int quantity) {
        var line = new OrderLineEntity();
        line.order = order;
        line.productId = product.getId();
        line.productSku = product.getSku();
        line.productName = product.getName();
        line.unitPrice = product.getPrice();
        line.quantity = quantity;
        line.lineAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        return line;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductSku() {
        return productSku;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineAmount() {
        return lineAmount;
    }
}
