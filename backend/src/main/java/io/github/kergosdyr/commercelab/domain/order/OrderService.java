package io.github.kergosdyr.commercelab.domain.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderPlacer orderPlacer;

    public OrderService(OrderPlacer orderPlacer) {
        this.orderPlacer = orderPlacer;
    }

    @Transactional
    public PlacedOrderResult placeOrder(CreateOrderCommand command) {
        return orderPlacer.place(command);
    }
}
