package challenge.commerce.domain.order;

import challenge.commerce.domain.catalog.ProductReader;
import challenge.commerce.domain.catalog.StockAllocator;
import challenge.commerce.infra.db.OrderEntity;
import challenge.commerce.infra.db.OrderItemEntity;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 품목별 가격 확인, 재고 확보, 주문 저장을 하나의 트랜잭션으로 처리한다. */
@Service
public class OrderService {
    private final ProductReader products;
    private final StockAllocator stock;
    private final OrderSaver orders;

    public OrderService(ProductReader products, StockAllocator stock, OrderSaver orders) {
        this.products = products;
        this.stock = stock;
        this.orders = orders;
    }

    @Transactional
    public OrderEntity create(CreateOrderCommand command) {
        var selections = products.readForOptions(
                command.items().stream().map(OrderItemCommand::optionId).toList());
        var items = command.items().stream()
                .map(item -> {
                    var selection = selections.get(item.optionId());
                    selection.product().validatePrice(item.displayedUnitPrice());
                    return OrderItemEntity.select(selection.product(), selection.option(), item.quantity());
                })
                .toList();
        // 서로 다른 순서로 같은 옵션을 주문해도 재고 행의 잠금 순서는 일정하게 유지한다.
        for (var item : command.items().stream()
                .sorted(Comparator.comparingLong(OrderItemCommand::optionId))
                .toList()) {
            stock.allocate(item.optionId(), item.quantity());
        }
        return orders.create(items);
    }
}
