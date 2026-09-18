package challenge.commerce.infra.db;

import challenge.commerce.domain.order.OrderRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryImpl implements OrderRepository {
    private final OrderJpaRepository orderJpaRepository;

    public OrderRepositoryImpl(OrderJpaRepository orderJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
    }

    @Override
    public OrderEntity create(OrderEntity order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<OrderEntity> findById(long id) {
        return orderJpaRepository.findWithItemsById(id);
    }

    @Override
    public List<OrderEntity> findAll() {
        return orderJpaRepository.findAllWithItems();
    }
}
