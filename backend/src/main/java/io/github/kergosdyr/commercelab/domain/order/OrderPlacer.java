package io.github.kergosdyr.commercelab.domain.order;

import java.time.Clock;

import io.github.kergosdyr.commercelab.domain.product.ProductReader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderPlacer {

    private final ProductReader productReader;
    private final OrderNumberIssuer orderNumberIssuer;
    private final OrderSaver orderSaver;
    private final Clock clock;

    public OrderPlacer(
            ProductReader productReader,
            OrderNumberIssuer orderNumberIssuer,
            OrderSaver orderSaver,
            Clock clock
    ) {
        this.productReader = productReader;
        this.orderNumberIssuer = orderNumberIssuer;
        this.orderSaver = orderSaver;
        this.clock = clock;
    }

    @Transactional
    public PlacedOrderResult place(CreateOrderCommand command) {
        var placedAt = clock.instant();
        var products = productReader.readProductsForUpdate(command.productIdsInLockOrder());
        var order = OrderEntity.place(orderNumberIssuer.issue(placedAt), command, products, placedAt);
        orderSaver.saveNewOrder(order);
        return PlacedOrderResult.from(order);
    }
}
