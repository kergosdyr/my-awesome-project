package challenge.commerce.infra.db;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "store_order")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    long optionId;
    String productName;
    String optionName;
    String image;
    long unitPrice;
    int quantity;
    long totalAmount;
    Instant createdAt;

    protected OrderEntity() {}

    public static OrderEntity place(ProductEntity product, ProductOptionEntity option, int quantity) {
        var order = new OrderEntity();
        order.optionId = option.id();
        order.productName = product.name();
        order.optionName = option.color() + " / " + option.size();
        order.image = product.image();
        order.unitPrice = product.price();
        order.quantity = quantity;
        order.totalAmount = Math.multiplyExact(product.price(), quantity);
        order.createdAt = Instant.now();
        return order;
    }

    public Long id() {
        return id;
    }

    public long optionId() {
        return optionId;
    }

    public String productName() {
        return productName;
    }

    public String optionName() {
        return optionName;
    }

    public String image() {
        return image;
    }

    public long unitPrice() {
        return unitPrice;
    }

    public int quantity() {
        return quantity;
    }

    public long totalAmount() {
        return totalAmount;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
