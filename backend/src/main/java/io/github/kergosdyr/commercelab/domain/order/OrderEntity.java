package io.github.kergosdyr.commercelab.domain.order;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.github.kergosdyr.commercelab.domain.product.ProductEntity;
import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String orderNumber;

    @Column(nullable = false, length = 40)
    private String customerName;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal totalAmount;

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = ALL, orphanRemoval = true, fetch = LAZY)
    private List<OrderLineEntity> lines = new ArrayList<>();

    protected OrderEntity() {
    }

    public static OrderEntity place(
            String orderNumber,
            CreateOrderCommand command,
            List<ProductEntity> lockedProducts,
            Instant placedAt
    ) {
        var productsById = new HashMap<Long, ProductEntity>();
        for (var product : lockedProducts) {
            productsById.put(product.getId(), product);
        }
        if (productsById.size() != command.lines().size()) {
            throw new ApiException(ErrorType.PRODUCT_NOT_FOUND);
        }

        var order = new OrderEntity();
        order.orderNumber = orderNumber;
        order.customerName = command.customerName();
        order.totalAmount = BigDecimal.ZERO;
        order.status = OrderStatus.PLACED;
        order.createdAt = placedAt;

        for (var requestedLine : command.lines()) {
            var product = productsById.get(requestedLine.productId());
            if (product == null) {
                throw new ApiException(ErrorType.PRODUCT_NOT_FOUND);
            }

            product.reserve(requestedLine.quantity());
            var line = OrderLineEntity.create(order, product, requestedLine.quantity());
            order.lines.add(line);
            order.totalAmount = order.totalAmount.add(line.getLineAmount());
        }
        return order;
    }

    List<OrderLineEntity> snapshotLines() {
        return List.copyOf(lines);
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
