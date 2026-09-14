package challenge.commerce.infra.db;

import challenge.commerce.domain.order.*;
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
    public PurchaseOrder create(PurchaseOrder order) {
        return orders.save(OrderEntity.from(order)).toDomain();
    }

    @Override
    public Optional<PurchaseOrder> findById(long id) {
        return orders.findById(id).map(OrderEntity::toDomain);
    }

    @Override
    public List<PurchaseOrder> findAll() {
        return orders.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(OrderEntity::toDomain)
                .toList();
    }
}
