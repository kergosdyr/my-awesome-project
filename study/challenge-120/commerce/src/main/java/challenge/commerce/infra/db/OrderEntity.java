package challenge.commerce.infra.db;

import challenge.commerce.domain.order.CreateOrderCommand;
import challenge.commerce.support.BusinessException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "store_order")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "item_position")
    private List<OrderItemEntity> items = new ArrayList<>();

    private long totalAmount;
    private Instant createdAt;

    protected OrderEntity() {}

    public static OrderEntity place(List<OrderItemEntity> items) {
        if (items.isEmpty() || items.size() > CreateOrderCommand.MAX_ITEMS) {
            throw BusinessException.invalid("주문 품목을 1~20개 선택해 주세요.");
        }
        var order = new OrderEntity();
        for (var item : items) {
            item.attachTo(order);
            order.items.add(item);
            order.totalAmount = Math.addExact(order.totalAmount, item.totalAmount());
        }
        order.createdAt = Instant.now();
        return order;
    }

    public Long id() {
        return id;
    }

    public List<OrderItemEntity> items() {
        return List.copyOf(items);
    }

    public long totalAmount() {
        return totalAmount;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
