package challenge.commerce.domain.order;

import challenge.commerce.domain.catalog.ProductReader;
import challenge.commerce.domain.catalog.StockAllocator;
import challenge.commerce.support.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public PurchaseOrder create(CreateOrder command) {
        var product = products.readForOption(command.optionId());
        stock.allocate(command.optionId(), command.quantity());
        return orders.create(product, command.optionId(), command.quantity());
    }

    public record CreateOrder(long optionId, int quantity) {
        public CreateOrder {
            if (optionId <= 0 || quantity < 1 || quantity > 5) {
                throw BusinessException.invalid("옵션과 수량(1~5개)을 확인해 주세요.");
            }
        }
    }
}
