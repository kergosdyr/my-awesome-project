package challenge.commerce.domain.order;

import challenge.commerce.infra.db.OrderEntity;
import challenge.commerce.infra.db.OrderItemEntity;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrderSaver {
    private final OrderRepository orders;

    public OrderSaver(OrderRepository orders) {
        this.orders = orders;
    }

    public OrderEntity create(List<OrderItemEntity> items) {
        return orders.create(OrderEntity.place(items));
    }
}
