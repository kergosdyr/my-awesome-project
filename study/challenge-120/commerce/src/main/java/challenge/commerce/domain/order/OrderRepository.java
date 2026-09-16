package challenge.commerce.domain.order;

import challenge.commerce.infra.db.OrderEntity;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    OrderEntity create(OrderEntity order);

    Optional<OrderEntity> findById(long id);

    List<OrderEntity> findAll();
}
