package challenge.commerce.domain.order;

import challenge.commerce.infra.db.OrderEntity;
import challenge.commerce.infra.db.OrderItemEntity;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrderSaver {
    private final OrderRepository orderRepository;

    public OrderSaver(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderEntity create(List<OrderItemEntity> items) {
        return orderRepository.create(OrderEntity.place(items));
    }
}
