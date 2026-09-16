package challenge.commerce.domain.order;

import challenge.commerce.domain.catalog.ProductResult;
import challenge.commerce.infra.db.OrderEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrderSaver {
    private final OrderRepository orders;

    public OrderSaver(OrderRepository orders) {
        this.orders = orders;
    }

    public OrderEntity create(ProductResult product, long optionId, int quantity) {
        var option = product.options().stream()
                .filter(o -> o.id() == optionId)
                .findFirst()
                .orElseThrow();
        return orders.create(OrderEntity.place(product.product(), option, quantity));
    }
}
