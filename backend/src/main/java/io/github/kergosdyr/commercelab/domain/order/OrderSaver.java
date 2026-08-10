package io.github.kergosdyr.commercelab.domain.order;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderSaver {

    private final OrderRepository orderRepository;

    public OrderSaver(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderEntity saveNewOrder(OrderEntity order) {
        return orderRepository.save(order);
    }
}
