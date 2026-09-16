package challenge.commerce.infra.db;

import challenge.commerce.domain.order.OrderRepository;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryImpl implements OrderRepository {
    private final OrderJpaRepository orders;

    public OrderRepositoryImpl(OrderJpaRepository orders) {
        this.orders = orders;
    }

    @Override
    public OrderEntity create(OrderEntity order) {
        return orders.save(order);
    }

    @Override
    public Optional<OrderEntity> findById(long id) {
        return orders.findById(id);
    }

    @Override
    public List<OrderEntity> findAll() {
        return orders.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }
}
