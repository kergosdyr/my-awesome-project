package challenge.commerce.domain.order;

import challenge.commerce.domain.query.PageQuery;
import challenge.commerce.infra.db.OrderEntity;
import java.util.Optional;

public interface OrderRepository {
    OrderEntity create(OrderEntity order);

    Optional<OrderEntity> findById(long id);

    Optional<OrderDetailsResult> findDetailsById(long id);

    OrderPageResult findPage(PageQuery query);

    OrderWindowResult findCursor(int size, OrderCursor cursor);
}
