package challenge.commerce.domain.order;

import challenge.commerce.domain.catalog.Product;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrderSaver {
    private final OrderRepository orders;

    public OrderSaver(OrderRepository orders) {
        this.orders = orders;
    }

    public PurchaseOrder create(Product product, long optionId, int quantity) {
        var option =
                product.options().stream()
                        .filter(o -> o.id() == optionId)
                        .findFirst()
                        .orElseThrow();
        return orders.create(
                new PurchaseOrder(
                        null,
                        optionId,
                        product.name(),
                        option.color() + " / " + option.size(),
                        product.image(),
                        product.price(),
                        quantity,
                        Math.multiplyExact(product.price(), quantity),
                        Instant.now()));
    }
}
