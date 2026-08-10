package io.github.kergosdyr.commercelab.infra.storage.mysql.order;

import io.github.kergosdyr.commercelab.domain.order.OrderEntity;
import io.github.kergosdyr.commercelab.domain.order.OrderRepository;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    public OrderRepositoryImpl(OrderJpaRepository orderJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
    }

    @Override
    public OrderEntity save(OrderEntity order) {
        return orderJpaRepository.save(order);
    }
}
