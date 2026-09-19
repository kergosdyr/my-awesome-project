package challenge.commerce.infra.db;

import jakarta.persistence.*;

@Entity
@Table(name = "store_order_item")
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    long optionId;
    String productName;
    String optionName;
    String image;
    long unitPrice;
    int quantity;
    long totalAmount;

    protected OrderItemEntity() {}

    public static OrderItemEntity select(ProductEntity product, ProductOptionEntity option, int quantity) {
        var item = new OrderItemEntity();
        item.optionId = option.id();
        item.productName = product.name();
        item.optionName = option.color() + " / " + option.size();
        item.image = product.image();
        item.unitPrice = product.price();
        item.quantity = quantity;
        item.totalAmount = Math.multiplyExact(product.price(), quantity);

        return item;
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

    void attachTo(OrderEntity order) {
        this.order = order;
    }
}
