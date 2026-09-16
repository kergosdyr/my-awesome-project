package challenge.commerce.infra.db;

import challenge.commerce.domain.order.PurchaseOrder;
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

    static OrderEntity from(PurchaseOrder o) {
        var e = new OrderEntity();
        e.id = o.id();
        e.optionId = o.optionId();
        e.productName = o.productName();
        e.optionName = o.optionName();
        e.image = o.image();
        e.unitPrice = o.unitPrice();
        e.quantity = o.quantity();
        e.totalAmount = o.totalAmount();
        e.createdAt = o.createdAt();
        return e;
    }

    PurchaseOrder toDomain() {
        return new PurchaseOrder(
                id, optionId, productName, optionName, image, unitPrice, quantity, totalAmount, createdAt);
    }
}
